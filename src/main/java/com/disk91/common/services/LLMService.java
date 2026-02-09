/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * LLMService - Provides LLM access with RAG capabilities supporting multiple providers (Ollama, OpenAI)
 * and multiple knowledge bases identified by unique keys.
 */
package com.disk91.common.services;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.interfaces.llm.KnowledgeBaseInfoResponseItf;
import com.disk91.common.interfaces.llm.KnowledgeDocumentBody;
import com.disk91.common.interfaces.llm.LlmQueryBody;
import com.disk91.common.interfaces.llm.LlmQueryResponseItf;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LLMService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected ChatClient.Builder chatClientBuilder;

    @Autowired
    protected VectorStore vectorStore;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // Metadata keys for knowledge base identification
    private static final String METADATA_KB_ID = "knowledge_base_id";
    private static final String METADATA_DOC_ID = "document_id";
    private static final String METADATA_TITLE = "title";
    private static final String METADATA_CATEGORY = "category";
    private static final String METADATA_SOURCE_URL = "source_url";
    private static final String METADATA_CREATED_AT = "created_at";

    // Cache for knowledge base info
    private final ConcurrentHashMap<String, KnowledgeBaseInfoResponseItf> knowledgeBaseCache = new ConcurrentHashMap<>();

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions based on the provided context.
            Use only the information from the context to answer questions.
            If the context doesn't contain relevant information, say that you don't have enough information to answer.
            Always be concise and accurate in your responses.
            
            Context:
            {context}
            """;

    /**
     * Query the LLM using RAG with a specific knowledge base
     * @param body - The query body containing knowledge base ID, query and optional parameters
     * @return LlmQueryResponseItf with the response and sources
     * @throws ITNotFoundException - When the knowledge base does not exist
     * @throws ITParseException - When query parameters are invalid
     */
    public LlmQueryResponseItf queryWithRag(LlmQueryBody body) throws ITNotFoundException, ITParseException {
        long startTime = System.currentTimeMillis();

        // Validate input parameters
        if (body.getKnowledgeBaseId() == null || body.getKnowledgeBaseId().isBlank()) {
            log.warn("[common][llm] Query attempt with empty knowledge base ID");
            throw new ITParseException("llm-kb-id-required");
        }
        if (body.getQuery() == null || body.getQuery().isBlank()) {
            log.warn("[common][llm] Query attempt with empty query");
            throw new ITParseException("llm-query-required");
        }

        // Check if knowledge base exists
        if (!knowledgeBaseExists(body.getKnowledgeBaseId())) {
            log.warn("[common][llm] Query attempt on non-existent knowledge base: {}", body.getKnowledgeBaseId());
            throw new ITNotFoundException("llm-kb-not-found");
        }

        log.debug("[common][llm] Processing RAG query for knowledge base: {}", body.getKnowledgeBaseId());

        // Determine topK value
        int topK = (body.getTopK() != null && body.getTopK() > 0) ? body.getTopK() : commonConfig.getLlmRagTopK();

        // Build filter for the specific knowledge base
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        var filter = filterBuilder.eq(METADATA_KB_ID, body.getKnowledgeBaseId()).build();

        // Create search request with filter
        SearchRequest searchRequest = SearchRequest.builder()
                .query(body.getQuery())
                .topK(topK)
                .similarityThreshold(commonConfig.getLlmRagSimilarityThreshold())
                .filterExpression(filter)
                .build();

        // Retrieve relevant documents
        List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);

        if (relevantDocs.isEmpty()) {
            log.info("[common][llm] No relevant documents found for query in knowledge base: {}", body.getKnowledgeBaseId());
        }

        // Build context from relevant documents
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // Build system prompt with context
        String baseSystemPrompt = (body.getSystemPrompt() != null && !body.getSystemPrompt().isBlank())
                ? body.getSystemPrompt() + "\n\nContext:\n{context}"
                : DEFAULT_SYSTEM_PROMPT;
        String systemPrompt = baseSystemPrompt.replace("{context}", context);

        // Create chat client and execute query
        ChatClient chatClient = chatClientBuilder.build();

        String response;
        long llmTimeoutMs = 60_000; // 60 seconds timeout
        try {
            // Execute LLM call with a timeout to avoid blocking indefinitely
            response = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .system(systemPrompt)
                            .user(body.getQuery())
                            .call()
                            .content()
            ).get(llmTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("[common][llm] LLM call timeout for KB {} after {}ms", body.getKnowledgeBaseId(), llmTimeoutMs);
            throw new ITParseException("llm-timeout");
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            log.error("[common][llm] LLM call failed for KB {}: {}", body.getKnowledgeBaseId(), e.getMessage());
            throw new ITParseException("llm-call-failed");
        }

        // Extract source document IDs
        List<String> sources = relevantDocs.stream()
                .map(doc -> (String) doc.getMetadata().get(METADATA_DOC_ID))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("[common][llm] RAG query completed for KB {} in {}ms, {} sources used",
                body.getKnowledgeBaseId(), processingTime, sources.size());

        // Build response
        LlmQueryResponseItf result = new LlmQueryResponseItf();
        result.setResponse(response);
        result.setSources(sources);
        result.setKnowledgeBaseId(body.getKnowledgeBaseId());
        result.setProcessingTimeMs(processingTime);

        return result;
    }

    /**
     * Add a document to a knowledge base
     * @param knowledgeBaseId - The knowledge base identifier
     * @param documentBody - The document to add
     * @throws ITParseException - When document parameters are invalid
     */
    public void addDocument(String knowledgeBaseId, KnowledgeDocumentBody documentBody) throws ITParseException {
        // Validate input parameters
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            log.warn("[common][llm] Add document attempt with empty knowledge base ID");
            throw new ITParseException("llm-kb-id-required");
        }
        if (documentBody.getDocumentId() == null || documentBody.getDocumentId().isBlank()) {
            log.warn("[common][llm] Add document attempt with empty document ID");
            throw new ITParseException("llm-doc-id-required");
        }
        if (documentBody.getContent() == null || documentBody.getContent().isBlank()) {
            log.warn("[common][llm] Add document attempt with empty content");
            throw new ITParseException("llm-doc-content-required");
        }

        log.debug("[common][llm] Adding document {} to knowledge base {}", documentBody.getDocumentId(), knowledgeBaseId);

        // Remove existing document with same ID if exists
        deleteDocument(knowledgeBaseId, documentBody.getDocumentId());

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(METADATA_KB_ID, knowledgeBaseId);
        metadata.put(METADATA_DOC_ID, documentBody.getDocumentId());
        metadata.put(METADATA_CREATED_AT, System.currentTimeMillis());

        if (documentBody.getTitle() != null && !documentBody.getTitle().isBlank()) {
            metadata.put(METADATA_TITLE, documentBody.getTitle());
        }
        if (documentBody.getCategory() != null && !documentBody.getCategory().isBlank()) {
            metadata.put(METADATA_CATEGORY, documentBody.getCategory());
        }
        if (documentBody.getSourceUrl() != null && !documentBody.getSourceUrl().isBlank()) {
            metadata.put(METADATA_SOURCE_URL, documentBody.getSourceUrl());
        }

        // Create and add document
        Document document = new Document(documentBody.getContent(), metadata);
        vectorStore.add(List.of(document));

        // Invalidate cache for this knowledge base
        knowledgeBaseCache.remove(knowledgeBaseId);

        log.info("[common][llm] Document {} added to knowledge base {}", documentBody.getDocumentId(), knowledgeBaseId);
    }

    /**
     * Add multiple documents to a knowledge base in batch
     * @param knowledgeBaseId - The knowledge base identifier
     * @param documents - List of documents to add
     * @throws ITParseException - When document parameters are invalid
     */
    public void addDocuments(String knowledgeBaseId, List<KnowledgeDocumentBody> documents) throws ITParseException {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            log.warn("[common][llm] Batch add documents attempt with empty knowledge base ID");
            throw new ITParseException("llm-kb-id-required");
        }
        if (documents == null || documents.isEmpty()) {
            log.warn("[common][llm] Batch add documents attempt with empty document list");
            throw new ITParseException("llm-docs-required");
        }

        log.info("[common][llm] Batch adding {} documents to knowledge base {}", documents.size(), knowledgeBaseId);

        List<Document> docsToAdd = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (KnowledgeDocumentBody docBody : documents) {
            if (docBody.getDocumentId() == null || docBody.getDocumentId().isBlank()) {
                throw new ITParseException("llm-doc-id-required");
            }
            if (docBody.getContent() == null || docBody.getContent().isBlank()) {
                throw new ITParseException("llm-doc-content-required");
            }

            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(METADATA_KB_ID, knowledgeBaseId);
            metadata.put(METADATA_DOC_ID, docBody.getDocumentId());
            metadata.put(METADATA_CREATED_AT, now);

            if (docBody.getTitle() != null && !docBody.getTitle().isBlank()) {
                metadata.put(METADATA_TITLE, docBody.getTitle());
            }
            if (docBody.getCategory() != null && !docBody.getCategory().isBlank()) {
                metadata.put(METADATA_CATEGORY, docBody.getCategory());
            }
            if (docBody.getSourceUrl() != null && !docBody.getSourceUrl().isBlank()) {
                metadata.put(METADATA_SOURCE_URL, docBody.getSourceUrl());
            }

            docsToAdd.add(new Document(docBody.getContent(), metadata));
        }

        // Add all documents in batch
        vectorStore.add(docsToAdd);

        // Invalidate cache
        knowledgeBaseCache.remove(knowledgeBaseId);

        log.info("[common][llm] Batch add completed: {} documents added to knowledge base {}", documents.size(), knowledgeBaseId);
    }

    /**
     * Delete a specific document from a knowledge base
     * @param knowledgeBaseId - The knowledge base identifier
     * @param documentId - The document identifier to delete
     */
    public void deleteDocument(String knowledgeBaseId, String documentId) {
        if (knowledgeBaseId == null || documentId == null) {
            return;
        }

        log.debug("[common][llm] Deleting document {} from knowledge base {}", documentId, knowledgeBaseId);

        // Find documents matching the criteria
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        var filter = filterBuilder.and(
                filterBuilder.eq(METADATA_KB_ID, knowledgeBaseId),
                filterBuilder.eq(METADATA_DOC_ID, documentId)
        ).build();

        SearchRequest searchRequest = SearchRequest.builder()
                .query("")
                .topK(100)
                .filterExpression(filter)
                .build();

        List<Document> docsToDelete = vectorStore.similaritySearch(searchRequest);
        if (!docsToDelete.isEmpty()) {
            List<String> ids = docsToDelete.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());
            vectorStore.delete(ids);
            knowledgeBaseCache.remove(knowledgeBaseId);
            log.info("[common][llm] Document {} deleted from knowledge base {}", documentId, knowledgeBaseId);
        }
    }

    /**
     * Delete all documents from a knowledge base
     * @param knowledgeBaseId - The knowledge base identifier
     * @throws ITNotFoundException - When the knowledge base does not exist
     */
    public void deleteKnowledgeBase(String knowledgeBaseId) throws ITNotFoundException {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            throw new ITNotFoundException("llm-kb-id-required");
        }

        if (!knowledgeBaseExists(knowledgeBaseId)) {
            throw new ITNotFoundException("llm-kb-not-found");
        }

        log.info("[common][llm] Deleting knowledge base: {}", knowledgeBaseId);

        // Find all documents in this knowledge base
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        var filter = filterBuilder.eq(METADATA_KB_ID, knowledgeBaseId).build();

        SearchRequest searchRequest = SearchRequest.builder()
                .query("")
                .topK(10000)
                .filterExpression(filter)
                .build();

        List<Document> docsToDelete = vectorStore.similaritySearch(searchRequest);
        if (!docsToDelete.isEmpty()) {
            List<String> ids = docsToDelete.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());
            vectorStore.delete(ids);
        }

        knowledgeBaseCache.remove(knowledgeBaseId);
        log.info("[common][llm] Knowledge base {} deleted, {} documents removed", knowledgeBaseId, docsToDelete.size());
    }

    /**
     * Get information about a specific knowledge base
     * @param knowledgeBaseId - The knowledge base identifier
     * @return KnowledgeBaseInfoResponseItf with knowledge base information
     * @throws ITNotFoundException - When the knowledge base does not exist
     */
    public KnowledgeBaseInfoResponseItf getKnowledgeBaseInfo(String knowledgeBaseId) throws ITNotFoundException {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            throw new ITNotFoundException("llm-kb-id-required");
        }

        // Check cache first
        KnowledgeBaseInfoResponseItf cached = knowledgeBaseCache.get(knowledgeBaseId);
        if (cached != null) {
            return cached;
        }

        // Query the vector store for document count and last sync
        Long documentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'knowledge_base_id' = ?",
                Long.class,
                knowledgeBaseId
        );

        if (documentCount == null || documentCount == 0) {
            throw new ITNotFoundException("llm-kb-not-found");
        }

        Long lastSync = jdbcTemplate.queryForObject(
                "SELECT MAX((metadata->>'created_at')::bigint) FROM vector_store WHERE metadata->>'knowledge_base_id' = ?",
                Long.class,
                knowledgeBaseId
        );

        KnowledgeBaseInfoResponseItf info = new KnowledgeBaseInfoResponseItf();
        info.setKnowledgeBaseId(knowledgeBaseId);
        info.setDocumentCount(documentCount);
        info.setLastSyncMs(lastSync != null ? lastSync : 0);

        // Cache the result
        knowledgeBaseCache.put(knowledgeBaseId, info);

        return info;
    }

    /**
     * List all available knowledge bases
     * @return List of knowledge base information
     */
    public List<KnowledgeBaseInfoResponseItf> listKnowledgeBases() {
        log.debug("[common][llm] Listing all knowledge bases");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT metadata->>'knowledge_base_id' as kb_id, " +
                        "COUNT(*) as doc_count, " +
                        "MAX((metadata->>'created_at')::bigint) as last_sync " +
                        "FROM vector_store " +
                        "GROUP BY metadata->>'knowledge_base_id'"
        );

        List<KnowledgeBaseInfoResponseItf> knowledgeBases = new ArrayList<>();
        for (Map<String, Object> row : results) {
            KnowledgeBaseInfoResponseItf info = new KnowledgeBaseInfoResponseItf();
            info.setKnowledgeBaseId((String) row.get("kb_id"));
            info.setDocumentCount(((Number) row.get("doc_count")).longValue());
            Object lastSync = row.get("last_sync");
            info.setLastSyncMs(lastSync != null ? ((Number) lastSync).longValue() : 0);
            knowledgeBases.add(info);
        }

        log.info("[common][llm] Found {} knowledge bases", knowledgeBases.size());
        return knowledgeBases;
    }

    /**
     * Simple text generation without RAG
     * @param prompt - The user prompt
     * @param systemPrompt - Optional system prompt
     * @return Generated text response
     */
    public String generateText(String prompt, String systemPrompt) {
        log.debug("[common][llm] Generating text without RAG");

        String effectiveSystemPrompt = (systemPrompt != null && !systemPrompt.isBlank())
                ? systemPrompt
                : "You are a helpful assistant.";

        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system(effectiveSystemPrompt)
                .user(prompt)
                .call()
                .content();

        log.debug("[common][llm] Text generation completed");
        return response;
    }

    /**
     * Check if a knowledge base exists
     * @param knowledgeBaseId - The knowledge base identifier
     * @return true if the knowledge base exists
     */
    public boolean knowledgeBaseExists(String knowledgeBaseId) {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
            return false;
        }

        // Check cache first
        if (knowledgeBaseCache.containsKey(knowledgeBaseId)) {
            return true;
        }

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'knowledge_base_id' = ?",
                Long.class,
                knowledgeBaseId
        );

        return count != null && count > 0;
    }

    /**
     * Get the current LLM provider name
     * @return The active provider name (ollama or openai)
     */
    public String getCurrentProvider() {
        return commonConfig.getLlmProvider();
    }
}

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

package com.disk91.common.services;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.LLMConfig;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Since different LLM providers use different vector stores for embeddings, we must route to the correct
 * vector store depending on the LLM. This routing class performs that operation transparently to simplify
 * integration in LLMService class
 */

@Service
public class LLMVectorStoreService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired(required = false)
    @Qualifier("ollamaVectorStore")
    private VectorStore ollamaVectorStore;

    @Autowired(required = false)
    @Qualifier("openaiVectorStore")
    private VectorStore openAiVectorStore;

    private final JdbcTemplate jdbcTemplate;

    public LLMVectorStoreService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public VectorStore getVectorStore() throws ITNotFoundException{
        if ( commonConfig.getLlmProvider().compareTo(CommonConfig.LLM_PROVIDER_OLLAMA) == 0 ) {
            if (ollamaVectorStore == null) {
                log.error("[common][llm] Ollama VectorStore is not available but provider is set to Ollama");
                throw new ITNotFoundException("common-llm-vectorstore-not-available");
            }
            return  ollamaVectorStore;
        } else if ( commonConfig.getLlmProvider().compareTo(CommonConfig.LLM_PROVIDER_OPENAI) == 0 ) {
            if (openAiVectorStore == null) {
                log.error("[common][llm] OpenAI VectorStore is not available but provider is set to OpenAI");
                throw new ITNotFoundException("common-llm-vectorstore-not-available");
            }
            return openAiVectorStore;
        } else {
            log.error("[common][llm] LLM Provider {} not supported", commonConfig.getLlmProvider());
            throw new ITNotFoundException("common-llm-provider-not-supported");
        }
    }

    public String getVectorStoreTableName() throws ITNotFoundException {
        if ( commonConfig.getLlmProvider().compareTo(CommonConfig.LLM_PROVIDER_OLLAMA) == 0 ) {
            return LLMConfig.VECTOR_STORE_OLLAMA;
        } else if ( commonConfig.getLlmProvider().compareTo(CommonConfig.LLM_PROVIDER_OPENAI) == 0 ) {
            return LLMConfig.VECTOR_STORE_OPENAI;
        } else {
            log.error("[common][llm] LLM Provider {} not supported", commonConfig.getLlmProvider());
            throw new ITNotFoundException("common-llm-provider-not-supported");
        }
    }

    // ============================================================================================
    // Database schema initialization for vector store (PostgreSQL with pgvector extension)
    // ============================================================================================

    @PostConstruct
    public void initLLMVectorStoreService() {

        log.info("[common][llm] Initializing vector store database schemas if not yet done");

        // Ollama vector store with 768 dimensions
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS public.%s (
              id       text PRIMARY KEY,
              content  text,
              metadata jsonb,
              embedding vector(768)
            );
        """.formatted(LLMConfig.VECTOR_STORE_OLLAMA));

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_ollama_embedding
            ON public.%s USING hnsw (embedding vector_cosine_ops);
        """.formatted(LLMConfig.VECTOR_STORE_OLLAMA));

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_ollama_kb_doc
            ON public.%s ((metadata->>'kbId'), (metadata->>'docId'));
        """.formatted(LLMConfig.VECTOR_STORE_OLLAMA));

        // OpenAI vector store with 1536 dimensions

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS public.%s (
              id       text PRIMARY KEY,
              content  text,
              metadata jsonb,
              embedding vector(1536)
            );
        """.formatted(LLMConfig.VECTOR_STORE_OPENAI));

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_openai_embedding
            ON public.%s USING hnsw (embedding vector_cosine_ops);
        """.formatted(LLMConfig.VECTOR_STORE_OPENAI));


        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_openai_kb_doc
            ON public.%s ((metadata->>'kbId'), (metadata->>'docId'));
        """.formatted(LLMConfig.VECTOR_STORE_OPENAI));

    }

}

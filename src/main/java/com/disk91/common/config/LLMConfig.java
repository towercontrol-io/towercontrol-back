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
package com.disk91.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI Configuration - Configures LLM providers (Ollama/OpenAI) and Vector Store
 */
@Configuration
public class LLMConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * Create the Chat Model based on the configured provider
     * @return ChatModel instance for the active provider
     */
    @Bean
    public ChatModel chatModel() {
        String provider = commonConfig.getLlmProvider();
        log.info("[common][llm] Initializing chat model with provider: {}", provider);

        if (CommonConfig.LLM_PROVIDER_OPENAI.equals(provider)) {
            return createOpenAiChatModel();
        } else if ( CommonConfig.LLM_PROVIDER_OLLAMA.equals(provider) ) {
            return createOllamaChatModel();
        }
        return null;
    }

    /**
     * Create the Embedding Model based on the configured provider
     * @return EmbeddingModel instance for the active provider
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        String provider = commonConfig.getLlmProvider();
        log.info("[common][llm] Initializing embedding model with provider: {}", provider);

        if (CommonConfig.LLM_PROVIDER_OPENAI.equals(provider)) {
            return createOpenAiEmbeddingModel();
        } else if ( CommonConfig.LLM_PROVIDER_OLLAMA.equals(provider) ) {
            // Default to Ollama
            return createOllamaEmbeddingModel();
        }
        return null;
    }

    /**
     * Create the ChatClient builder
     * @param chatModel - The chat model to use
     * @return ChatClient.Builder instance
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }

    /**
     * Create the PgVector Store for RAG
     * @param embeddingModel - The embedding model to use
     * @return VectorStore instance using PostgreSQL pgvector
     */
    /*
    @Bean
    @Primary
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("[common][llm] Initializing PgVector store for RAG");
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("llm_vector_store")
                .build();
    }
    */


    // ==========================================
    // Private methods for provider instantiation

    private ChatModel createOllamaChatModel() {
        log.info("[common][llm] Creating Ollama chat model with URL: {} and model: {}",
                commonConfig.getLlmOllamaUrl(), commonConfig.getLlmOllamaChatModel());

        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(commonConfig.getLlmOllamaUrl())
                .build();

        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(commonConfig.getLlmOllamaChatModel())
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }

    private ChatModel createOpenAiChatModel() {
        log.info("[common][llm] Creating OpenAI chat model with model: {}", commonConfig.getLlmOpenAiChatModel());

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(commonConfig.getLlmOpenAiUrl())
                .apiKey(commonConfig.getLlmOpenAiKey())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(commonConfig.getLlmOpenAiChatModel())
                        .build())
                .build();
    }

    private EmbeddingModel createOllamaEmbeddingModel() {
        log.info("[common][llm] Creating Ollama embedding model with URL: {} and model: {}",
                commonConfig.getLlmOllamaUrl(), commonConfig.getLlmOllamaEmbeddingModel());

        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(commonConfig.getLlmOllamaUrl())
                .build();

        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model(commonConfig.getLlmOllamaEmbeddingModel())
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }

    private EmbeddingModel createOpenAiEmbeddingModel() {
        log.info("[common][llm] Creating OpenAI embedding model with model: {}", commonConfig.getLlmOpenAiEmbeddingModel());

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(commonConfig.getLlmOpenAiUrl())
                .apiKey(commonConfig.getLlmOpenAiKey())
                .build();
        return new OpenAiEmbeddingModel(
                openAiApi,
                org.springframework.ai.document.MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(commonConfig.getLlmOpenAiEmbeddingModel())
                        .build()
        );
    }
}

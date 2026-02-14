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

import com.disk91.common.tools.exceptions.ITNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI Configuration - Configures LLM providers (Ollama/OpenAI)
 * This will route the AI interaction to the right provider base on configuration file
 *
 */
@Configuration
public class LLMConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    /**
     * Create the Chat Model based on the configured provider
     * @return ChatModel instance for the active provider
     */
    @Bean
    public ChatModel chatModel() throws ITNotFoundException {
        String provider = commonConfig.getLlmProvider();
        log.info("[common][llm] Initializing chat model with provider: {}", provider);

        if (CommonConfig.LLM_PROVIDER_OPENAI.equals(provider)) {
            return createOpenAiChatModel();
        } else if ( CommonConfig.LLM_PROVIDER_OLLAMA.equals(provider) ) {
            return createOllamaChatModel();
        }
        throw new ITNotFoundException("common-llm-provider-not-set");
    }

    /**
     * Create the Embedding Model based on the configured provider
     * @return EmbeddingModel instance for the active provider
     */
    @Bean
    public EmbeddingModel embeddingModel() throws ITNotFoundException {
        String provider = commonConfig.getLlmProvider();
        log.info("[common][llm] Initializing embedding model with provider: {}", provider);

        if (CommonConfig.LLM_PROVIDER_OPENAI.equals(provider)) {
            return createOpenAiEmbeddingModel();
        } else if ( CommonConfig.LLM_PROVIDER_OLLAMA.equals(provider) ) {
            return createOllamaEmbeddingModel();
        }
        throw new ITNotFoundException("common-llm-provider-not-set");
    }

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

    // =============================================================
    // MANAGE VECTOR STORE
    // -----------------------------------------------
    // OpenAI and Ollama doesn't have the same vector store constraint, using
    // different kind of vector size, so we need to create dedicated vector store for each,
    // based on configuration and route the call to the right one, depending on the configuration

    public static final String VECTOR_STORE_OLLAMA = "common_llm_ollama_vector_store";
    public static final String VECTOR_STORE_OPENAI = "common_llm_openai_vector_store";

    @Bean
    @Qualifier("ollamaEmbeddingModel")
    @ConditionalOnProperty(name = "common.llm.provider", havingValue = "ollama")
    public EmbeddingModel ollamaEmbeddingModel(/* deps ollama */) {
        return createOllamaEmbeddingModel();
    }

    @Bean
    @Qualifier("openAiEmbeddingModel")
    @ConditionalOnProperty(name = "common.llm.provider", havingValue = "openai")
    public EmbeddingModel openAiEmbeddingModel(/* deps openai */) {
        return createOpenAiEmbeddingModel();
    }

    @Bean
    @Qualifier("ollamaVectorStore")
    @ConditionalOnProperty(name = "common.llm.provider", havingValue = "ollama")
    public VectorStore ollamaVectorStore(
            JdbcTemplate jdbcTemplate,
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(768)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false)
                .schemaName("public")
                .vectorTableName(VECTOR_STORE_OLLAMA)
                .build();
    }

    @Bean
    @Qualifier("openaiVectorStore")
    @ConditionalOnProperty(name = "common.llm.provider", havingValue = "openai")
    public VectorStore openaiVectorStore(
            JdbcTemplate jdbcTemplate,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false)
                .schemaName("public")
                .vectorTableName(VECTOR_STORE_OPENAI)
                .build();
    }

}

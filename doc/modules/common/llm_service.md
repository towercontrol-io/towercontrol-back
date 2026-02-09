# LLM Service Documentation

## Overview

The LLM Service provides access to Large Language Models with RAG (Retrieval-Augmented Generation) capabilities. 
It supports multiple LLM providers (Ollama, OpenAI) and manages multiple knowledge bases identified by unique keys.

## Configuration

### Properties

Add the following properties to your `common.properties` file:

```properties
# LLM Provider (ollama or openai)
common.llm.provider=${COMMON_LLM_PROVIDER:ollama}

# Ollama Configuration
common.llm.ollama.url=${COMMON_LLM_OLLAMA_URL:http://localhost:11434}
common.llm.ollama.chat.model=${COMMON_LLM_OLLAMA_CHAT_MODEL:llama3}
common.llm.ollama.embedding.model=${COMMON_LLM_OLLAMA_EMBEDDING_MODEL:nomic-embed-text}

# OpenAI Configuration
common.llm.openai.url=${COMMON_LLM_OPENAI_URL:https://api.openai.com/v1}
common.llm.openai.key=${COMMON_LLM_OPENAI_KEY:}
common.llm.openai.chat.model=${COMMON_LLM_OPENAI_CHAT_MODEL:gpt-4o-mini}
common.llm.openai.embedding.model=${COMMON_LLM_OPENAI_EMBEDDING_MODEL:text-embedding-3-small}

# RAG Configuration
common.llm.rag.topk=${COMMON_LLM_RAG_TOPK:5}
common.llm.rag.similarity.threshold=${COMMON_LLM_RAG_SIMILARITY_THRESHOLD:0.7}
```

### PostgreSQL pgvector Extension

The LLM Service uses PostgreSQL with the pgvector extension for vector storage. 
You need to install the pgvector extension in your PostgreSQL database:

```shell
# Only if the installation was made before 2026-02-09
# Connect to your database as superuser and run:
docker exec -it itc_run-postgres-1 psql -U postgres --dbname="itc" -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

The service will automatically create the required table `vector_store` on startup.

## Usage

### Inject the Service

```java
@Autowired
protected LLMService llmService;
```

### Add Documents to a Knowledge Base

```java
// Add a single document
KnowledgeDocumentBody doc = new KnowledgeDocumentBody();
doc.setDocumentId("doc-001");
doc.setContent("This is the document content...");
doc.setTitle("Document Title");
doc.setCategory("user-guide");
doc.setSourceUrl("https://docs.example.com/doc-001");

llmService.addDocument("my-knowledge-base", doc);

// Add multiple documents in batch
List<KnowledgeDocumentBody> documents = new ArrayList<>();
// ... populate documents list
llmService.addDocuments("my-knowledge-base", documents);
```

### Query with RAG

```java
LlmQueryBody query = new LlmQueryBody();
query.setKnowledgeBaseId("my-knowledge-base");
query.setQuery("How do I configure my device?");
query.setSystemPrompt("You are a helpful assistant specialized in IoT devices."); // Optional
query.setTopK(5); // Optional, defaults to config value

LlmQueryResponseItf response = llmService.queryWithRag(query);
String answer = response.getResponse();
List<String> sources = response.getSources();
```

### Simple Text Generation (without RAG)

```java
String response = llmService.generateText(
    "What is the capital of France?",
    "You are a helpful assistant." // Optional system prompt
);
```

### Manage Knowledge Bases

```java
// List all knowledge bases
List<KnowledgeBaseInfoResponseItf> kbs = llmService.listKnowledgeBases();

// Get info about a specific knowledge base
KnowledgeBaseInfoResponseItf info = llmService.getKnowledgeBaseInfo("my-knowledge-base");

// Check if knowledge base exists
boolean exists = llmService.knowledgeBaseExists("my-knowledge-base");

// Delete a document
llmService.deleteDocument("my-knowledge-base", "doc-001");

// Delete entire knowledge base
llmService.deleteKnowledgeBase("my-knowledge-base");
```

## Supported Providers

### Ollama

Ollama is an open-source LLM runtime. To use Ollama:

1. Install Ollama: https://ollama.ai
2. Pull required models:
   ```bash
   ollama pull llama3
   ollama pull nomic-embed-text
   ```
3. Configure the service to use Ollama (default)

### OpenAI

To use OpenAI:

1. Get an API key from https://platform.openai.com
2. Set the configuration:
   ```properties
   common.llm.provider=openai
   common.llm.openai.key=sk-your-api-key
   ```

## Error Messages (i18n slugs)

| Slug | Description |
|------|-------------|
| `common-llm-kb-id-required` | Knowledge base ID is required but was not provided |
| `common-llm-query-required` | Query text is required but was not provided |
| `common-llm-kb-not-found` | The specified knowledge base does not exist |
| `common-llm-doc-id-required` | Document ID is required but was not provided |
| `common-llm-doc-content-required` | Document content is required but was not provided |
| `common-llm-docs-required` | Document list is required but was not provided or empty |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        LLMService                           │
├─────────────────────────────────────────────────────────────┤
│  - queryWithRag()                                           │
│  - addDocument() / addDocuments()                           │
│  - deleteDocument() / deleteKnowledgeBase()                 │
│  - getKnowledgeBaseInfo() / listKnowledgeBases()           │
│  - generateText()                                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
          ┌────────────────┴────────────────┐
          │                                 │
          ▼                                 ▼
┌─────────────────────┐         ┌─────────────────────┐
│     LLMConfig       │         │    PgVectorStore    │
├─────────────────────┤         ├─────────────────────┤
│  - ChatModel        │         │  - PostgreSQL       │
│  - EmbeddingModel   │         │  - pgvector ext     │
│  - ChatClient       │         │  - HNSW index       │
└─────────┬───────────┘         └─────────────────────┘
          │
    ┌─────┴─────┐
    │           │
    ▼           ▼
┌────────┐  ┌────────┐
│ Ollama │  │ OpenAI │
└────────┘  └────────┘
```

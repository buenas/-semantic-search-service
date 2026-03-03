# Semantic Search Service

A production-ready REST API for semantic document search built with Spring Boot, PostgreSQL, and pgvector. Documents are stored with OpenAI vector embeddings, enabling similarity-based search using cosine distance — going beyond keyword matching to find results that are conceptually related to a query.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Running the Tests](#running-the-tests)
- [Future Improvements](#future-improvements)

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 / Spring Boot 3 | Application framework |
| PostgreSQL 16 + [pgvector](https://github.com/pgvector/pgvector) | Vector storage and cosine distance queries |
| OpenAI Embeddings API (`text-embedding-3-small`) | Text → vector conversion |
| Spring Data JPA + JdbcTemplate | Database access |
| Flyway | Database migrations |
| Docker Compose | Local development |
| JUnit 5 + Mockito + MockMvc | Testing |

---

## Project Structure

```
semantic-search-service/
├── infra/
│   ├── docker-compose.yml          # Runs Postgres + app together
│   └── docker-compose.dev.yml      # Runs Postgres only (for local development)
├── src/
│   ├── main/
│   │   ├── java/com/semantic_search_service/
│   │   │   ├── controller/
│   │   │   │   ├── dto/                        # Request and response DTOs
│   │   │   │   │   ├── CreateDocumentRequest.java
│   │   │   │   │   ├── CreateDocumentResponse.java
│   │   │   │   │   ├── DocumentResponse.java
│   │   │   │   │   ├── UpdateDocumentRequest.java
│   │   │   │   │   ├── SearchRequest.java
│   │   │   │   │   ├── SearchResponse.java
│   │   │   │   │   └── SearchResultItem.java
│   │   │   │   ├── validation/
│   │   │   │   │   ├── JsonObjectValidator.java # Validates metadata is a JSON object
│   │   │   │   │   └── JsonValidator.java       # Custom @JsonValidator annotation
│   │   │   │   ├── DocumentController.java      # POST, GET, PUT, DELETE /documents
│   │   │   │   ├── HealthController.java        # GET /ping
│   │   │   │   └── SearchController.java        # POST /search
│   │   │   ├── domain/
│   │   │   │   ├── Document.java                # JPA entity
│   │   │   │   └── DocumentStatus.java          # PENDING | READY | FAILED
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java  # Maps exceptions to HTTP error responses
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── repository/
│   │   │   │   └── DocumentRepository.java      # Spring Data JPA repository
│   │   │   ├── service/
│   │   │   │   ├── embedding/
│   │   │   │   │   ├── EmbeddingClient.java     # Interface — decouples OpenAI from service
│   │   │   │   │   ├── EmbeddingUtils.java      # L2 normalisation utility
│   │   │   │   │   └── OpenAiEmbeddingClient.java
│   │   │   │   ├── impl/
│   │   │   │   │   └── DocumentServiceImpl.java # All business logic
│   │   │   │   └── DocumentService.java         # Service interface
│   │   │   └── SemanticSearchServiceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           ├── V1__init.sql
│   │           ├── V2__document_status.sql
│   │           └── V3__normalize_metadata_jsonb.sql
│   └── test/
│       ├── java/com/semantic_search_service/
│       │   ├── DocumentControllerIntegrationTest.java
│       │   ├── DocumentServiceImplTest.java
│       │   ├── EmbeddingUtilsTest.java
│       │   └── SemanticSearchServiceApplicationTests.java
│       └── resources/
│           └── application-test.properties
├── Dockerfile
├── .gitignore
└── pom.xml
```

---

## Architecture

The project follows a four-layer architecture where each layer has a single responsibility and only communicates with the layer directly below it.

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────┐
│             Controller Layer             │
│  DocumentController  SearchController   │
│  HealthController                        │
│  GlobalExceptionHandler                  │
│  → Validates input (@Valid)              │
│  → Maps exceptions to error responses   │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│              Service Layer               │
│  DocumentService (interface)             │
│  DocumentServiceImpl                     │
│  → All business logic                   │
│  → Orchestrates repository + embeddings │
│  → Manages document lifecycle           │
└──────────┬──────────────────┬───────────┘
           │                  │
           ▼                  ▼
┌──────────────────┐ ┌────────────────────┐
│  Repository Layer│ │  Embedding Layer   │
│  DocumentRepo    │ │  EmbeddingClient   │
│  (Spring Data    │ │  (interface)       │
│   JPA + JDBC)    │ │  OpenAiEmbedding   │
│                  │ │  Client            │
│                  │ │  EmbeddingUtils    │
└────────┬─────────┘ └──────────┬─────────┘
         │                      │
         ▼                      ▼
┌──────────────────┐ ┌────────────────────┐
│   PostgreSQL     │ │   OpenAI API       │
│   + pgvector     │ │                    │
└──────────────────┘ └────────────────────┘
```

### Key design decisions

**`EmbeddingClient` is an interface** — the rest of the application never imports `OpenAiEmbeddingClient` directly. This means the OpenAI provider can be swapped for a local model or any other provider without touching the service layer.

**JdbcTemplate alongside JPA** — Spring Data JPA handles standard CRUD operations. Raw `JdbcTemplate` is used for vector operations (`<=>` cosine distance, `::vector` casting) because JPA has no native understanding of pgvector syntax.

**Document lifecycle** — every document moves through three states: `PENDING` (saved, not yet embedded) → `READY` (embedded, searchable) → `FAILED` (embedding error, not searchable). The error message is stored in the database for debugging.

---

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21+
- Maven
- An [OpenAI API key](https://platform.openai.com/account/api-keys)

### Run locally (recommended for development)

This starts only Postgres in Docker and runs the Spring Boot app from your terminal or IDE:

```bash
# 1. Clone the repo
git clone https://github.com/YOUR_USERNAME/semantic-search-service.git
cd semantic-search-service

# 2. Start Postgres with pgvector
docker-compose -f infra/docker-compose.dev.yml up -d

# 3. Set your OpenAI key — use single quotes to avoid shell issues
export OPENAI_API_KEY='sk-your-key-here'

# 4. Run the app
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

> **Note:** Never commit your API key. Use `export` in your terminal or set it in your IDE's run configuration environment variables.

---

## Configuration

| Environment Variable | Required | Description | Default |
|---|---|---|---|
| `OPENAI_API_KEY` | Yes | Your OpenAI API key | — |
| `OPENAI_EMBEDDING_MODEL` | No | Embedding model name | `text-embedding-3-small` |

All other configuration lives in `src/main/resources/application.properties`. The test environment uses `src/test/resources/application-test.properties` with an in-memory H2 database and dummy values — no real key or database needed to run tests.

---

## API Reference

### Health Check

```
GET /ping
```

Returns `Ok` if the service is running.

---

### Create a Document

```
POST /documents
Content-Type: application/json
```

**Request:**
```json
{
  "title": "Introduction to Vector Databases",
  "content": "Vector databases store high-dimensional embeddings and retrieve similar items using approximate nearest neighbour algorithms...",
  "metadata": {
    "category": "technology",
    "author": "ozioma",
    "difficulty": "beginner"
  }
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "status": "READY"
}
```

The `Location` response header points to the created resource: `Location: /documents/1`

Status will be `FAILED` if the OpenAI embedding call fails. The error is stored in the database against the document.

| Field | Constraint |
|---|---|
| `title` | Required, not blank |
| `content` | Required, not blank |
| `metadata` | Optional — must be a JSON object if provided, not a string or array |

---

### Get a Document by ID

```
GET /documents/{id}
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "title": "Introduction to Vector Databases",
  "content": "Vector databases store high-dimensional embeddings...",
  "metadata": {
    "category": "technology",
    "author": "ozioma",
    "difficulty": "beginner"
  }
}
```

Returns `404 Not Found` if the document does not exist.

---

### Update a Document

```
PUT /documents/{id}
Content-Type: application/json
```

**Request** (all fields required):
```json
{
  "title": "Vector Databases: A Deep Dive",
  "content": "This updated guide covers HNSW and IVFFlat indexing strategies...",
  "metadata": {
    "category": "technology",
    "author": "ozioma",
    "difficulty": "advanced"
  }
}
```

**Response `200 OK`** — returns the updated document in the same shape as Get by ID.

Re-computes the embedding for the updated content. The document is set back to `PENDING` during re-embedding and returns to `READY` on success.

Returns `404 Not Found` if the document does not exist.

---

### Delete a Document

```
DELETE /documents/{id}
```

**Response `204 No Content`** — empty body.

Returns `404 Not Found` if the document does not exist.

---

### Semantic Search

```
POST /search
Content-Type: application/json
```

**Request:**
```json
{
  "query": "how do vector databases find similar items?",
  "page": 0,
  "size": 10,
  "minScore": 0.7,
  "filters": {
    "category": "technology"
  }
}
```

**Response `200 OK`:**
```json
{
  "page": 0,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1,
  "items": [
    {
      "id": 1,
      "title": "Introduction to Vector Databases",
      "content": "...",
      "metadata": { "category": "technology" },
      "cosineDistance": 0.12,
      "cosineSimilarity": 0.88,
      "score": 0.94
    }
  ]
}
```

| Field | Description | Constraint |
|---|---|---|
| `query` | The search query text | Required |
| `page` | Page number (zero-based) | Min 0 |
| `size` | Results per page | Min 1, Max 100 |
| `minScore` | Minimum score threshold to filter weak matches | 0.0 – 1.0, optional |
| `filters` | Exact match filters against top-level metadata keys | Optional |

**Scoring explained:**

| Field | Description |
|---|---|
| `cosineDistance` | Raw pgvector output (`<=>` operator) — lower means more similar |
| `cosineSimilarity` | `1 - cosineDistance` — higher means more similar |
| `score` | Normalised to `[0, 1]` using `(cosineSimilarity + 1) / 2` — suitable for display |

---

### Error Responses

All errors return a consistent JSON shape:

```json
{
  "timestamp": "2026-03-01T22:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Document not found: 99"
}
```

| Status | Cause |
|---|---|
| `400 Bad Request` | Validation failure — missing required fields, invalid metadata type, out-of-range values |
| `404 Not Found` | Document ID does not exist |
| `500 Internal Server Error` | Unexpected error — check application logs |

---

## Database Schema

Managed by Flyway — migrations run automatically on startup.

| Migration | Description |
|---|---|
| `V1__init.sql` | Creates `documents` table, enables pgvector extension, creates IVFFlat cosine index, adds `updated_at` trigger |
| `V2__document_status.sql` | Adds `status`, `embedding_error`, `embedding_updated_at` columns |
| `V3__normalize_metadata_jsonb.sql` | Data migration — converts any legacy string metadata to `{"raw": "<value>"}` JSON objects |

**Key columns:**

| Column | Type | Description |
|---|---|---|
| `id` | `BIGSERIAL` | Primary key |
| `title` | `TEXT` | Document title |
| `content` | `TEXT` | Document body |
| `metadata` | `JSONB` | Arbitrary key-value pairs for filtering |
| `embedding` | `VECTOR(1536)` | OpenAI embedding — adjust dimension for other models |
| `status` | `TEXT` | `PENDING` / `READY` / `FAILED` |
| `embedding_error` | `TEXT` | Stores the error message if embedding failed |
| `created_at` | `TIMESTAMPTZ` | Set automatically on insert |
| `updated_at` | `TIMESTAMPTZ` | Updated automatically via DB trigger |

The IVFFlat index (`lists = 100`) enables fast approximate nearest neighbour search. For datasets over 1 million rows, consider switching to an HNSW index for better recall.

---

## Running the Tests

No real database or OpenAI key needed — tests use an in-memory H2 database and mock all external calls.

```bash
./mvnw test
```

### Test classes

| Class | Type | What it covers |
|---|---|---|
| `EmbeddingUtilsTest` | Unit | L2 normalisation — correctness, zero vector, input immutability |
| `DocumentServiceImplTest` | Unit | Full service layer with mocked repository, JdbcTemplate, and EmbeddingClient |
| `DocumentControllerIntegrationTest` | Integration | Full MVC layer — HTTP status codes, request validation, error response shape |
| `SemanticSearchServiceApplicationTests` | Integration | Spring context loads without errors |

---

## Future Improvements

- **Async embedding** — `POST /documents` currently blocks the request thread until OpenAI responds (~1–2s). A message queue (Redis Streams, RabbitMQ) would decouple document ingestion from embedding computation
- **Retry with backoff** — add exponential backoff for OpenAI 429 (rate limit) and 503 responses using Spring Retry
- **`GET /documents`** — paginated endpoint to list all documents
- **HNSW index** — switch from IVFFlat to HNSW for better recall at scale with no query-time parameter tuning
- **Document chunking** — split documents that exceed OpenAI's token limit into chunks, embed each separately, and aggregate results at query time
- **Multi-tenancy** — scope documents by tenant ID stored in the `metadata` JSONB column

---

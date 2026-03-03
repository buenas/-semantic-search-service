package com.semantic_search_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semantic_search_service.controller.dto.*;
import com.semantic_search_service.domain.Document;
import com.semantic_search_service.domain.DocumentStatus;
import com.semantic_search_service.exception.ResourceNotFoundException;
import com.semantic_search_service.repository.DocumentRepository;
import com.semantic_search_service.service.DocumentService;
import com.semantic_search_service.service.embedding.EmbeddingClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;


    private static final String SQL_UPDATE_EMBEDDING = """
            UPDATE documents
            SET embedding = ?::vector,
                status = 'READY',
                embedding_error = NULL,
                embedding_updated_at = now()
            WHERE id = ?
            """;

    private static final String SQL_UPDATE_FAILED = """
            UPDATE documents
            SET status = 'FAILED',
                embedding_error = ?,
                embedding_updated_at = now()
            WHERE id = ?
            """;

    private static final String SQL_SEARCH_INNER = """
            SELECT id, title, content, metadata,
                   (embedding <=> ?::vector) AS cosine_distance
            FROM documents
            WHERE status = 'READY'
              AND embedding IS NOT NULL
            """;

    private static final String SQL_COUNT_INNER = """
            SELECT (embedding <=> ?::vector) AS cosine_distance
            FROM documents
            WHERE status = 'READY'
              AND embedding IS NOT NULL
            """;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               EmbeddingClient embeddingClient,
                               JdbcTemplate jdbcTemplate,
                               ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.embeddingClient = embeddingClient;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    //create document
    @Override
    @Transactional
    public CreateDocumentResponse create(CreateDocumentRequest request) {
        Document saved = saveAsPending(request);
        embedAndPersist(saved.getId(), saved.getTitle(), saved.getContent());
        return new CreateDocumentResponse(saved.getId(), DocumentStatus.READY);
    }

    private Document saveAsPending(CreateDocumentRequest request) {
        Document doc = new Document();
        doc.setTitle(request.getTitle());
        doc.setContent(request.getContent());
        doc.setMetadata(request.getMetadata());
        doc.setStatus(DocumentStatus.PENDING);
        return documentRepository.save(doc);
    }

    //Read doc
    @Override
    public DocumentResponse getById(Long id) {
        return documentRepository.findById(id)
                .map(this::toDocumentResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    // update document
    @Override
    @Transactional
    public DocumentResponse update(Long id, UpdateDocumentRequest request) {
        Document doc = findOrThrow(id);
        applyUpdates(doc, request);
        embedAndPersist(id, request.getTitle(), request.getContent());
        return toDocumentResponse(findOrThrow(id));
    }

    private void applyUpdates(Document doc, UpdateDocumentRequest request) {
        doc.setTitle(request.getTitle());
        doc.setContent(request.getContent());
        doc.setMetadata(request.getMetadata());
        doc.setStatus(DocumentStatus.PENDING);
        doc.setEmbeddingError(null);
        documentRepository.save(doc);
    }

    //delete
    @Override
    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        documentRepository.deleteById(id);
    }

    //search
    @Override
    public SearchResponse search(SearchRequest request) {
        String qVector = embedQuery(request.getQuery());
        List<SearchResultItem> items = fetchResults(request, qVector);
        int total = countResults(qVector, request.getFilters(), request.getMinScore());
        return new SearchResponse(request.getPage(), request.getSize(), total, items);
    }

    private String embedQuery(String query) {
        return toPgVectorLiteral(embeddingClient.embed(query));
    }

    private List<SearchResultItem> fetchResults(SearchRequest request, String qVector) {
        QueryBuilder inner = new QueryBuilder(SQL_SEARCH_INNER, qVector);
        inner.applyFilters(request.getFilters());

        String outerSql = "SELECT * FROM (\n" + inner.sql() + ") AS sub\n";
        QueryBuilder outer = new QueryBuilder(outerSql, inner);
        outer.applyMinScore(request.getMinScore());
        outer.applyPagination(request.getSize(), request.getPage() * request.getSize());
        return jdbcTemplate.query(outer.sql(), this::mapToSearchResultItem, outer.params());
    }

    private int countResults(String qVector, Map<String, String> filters, Double minScore) {
        //build inner
        QueryBuilder inner = new QueryBuilder(SQL_COUNT_INNER, qVector);
        inner.applyFilters(filters);
        String countSql = "SELECT COUNT(*) FROM (\n" + inner.sql() + ") AS sub\n";
        QueryBuilder countQb = new QueryBuilder(countSql, inner);
        countQb.applyMinScore(minScore);
        Integer count = jdbcTemplate.queryForObject(countQb.sql(), Integer.class, countQb.params());
        return count != null ? count : 0;
    }

    private SearchResultItem mapToSearchResultItem(java.sql.ResultSet rs, int rowNum)
            throws java.sql.SQLException {
        double dist = rs.getDouble("cosine_distance");
        double cosSim = 1.0 - dist;
        double score = (cosSim + 1.0) / 2.0;
        return new SearchResultItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                readMetadataAsJsonNode(rs.getObject("metadata")),
                dist, cosSim, score);
    }

    //embedding
    private void embedAndPersist(Long documentId, String title, String content) {
        try {
            float[] embedding = embeddingClient.embed(title + "\n\n" + content);
            int updated = jdbcTemplate.update(SQL_UPDATE_EMBEDDING,
                    toPgVectorLiteral(embedding), documentId);
            if (updated != 1) {
                throw new IllegalStateException(
                        "Unexpected row count updating embedding for document id=" + documentId);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            markFailed(documentId, e.getMessage());
            throw new RuntimeException("Embedding failed for document id=" + documentId, e);
        }
    }

    private void markFailed(Long documentId, String errorMessage) {
        String err = errorMessage != null && errorMessage.length() > 2000
                ? errorMessage.substring(0, 2000)
                : errorMessage;
        jdbcTemplate.update(SQL_UPDATE_FAILED, err, documentId);
    }

    private Document findOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    private DocumentResponse toDocumentResponse(Document doc) {
        return new DocumentResponse(doc.getId(),
                doc.getTitle(), doc.getContent(),
                doc.getMetadata());
    }

    private JsonNode readMetadataAsJsonNode(Object dbValue) {
        if (dbValue == null) return null;
        try {
            return objectMapper.readTree(dbValue.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static String toPgVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }
//QueryBuilder — encapsulates dynamic SQL + param construction for search
    private static class QueryBuilder {

        private final StringBuilder sql;
        private final List<Object> params = new ArrayList<>();

        QueryBuilder(String baseSql, String firstParam) {
            this.sql = new StringBuilder(baseSql);
            this.params.add(firstParam);
        }

        QueryBuilder(String baseSql, QueryBuilder source) {
            this.sql = new StringBuilder(baseSql);
            this.params.addAll(source.params);
        }

        void applyFilters(Map<String, String> filters) {
            if (filters == null || filters.isEmpty()) return;
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String key = entry.getKey();
                if (key == null || !key.matches("^[a-zA-Z0-9_-]{1,64}$")) {
                    throw new IllegalArgumentException("Invalid metadata filter key: " + key);
                }
                sql.append("  AND (metadata->>'").append(key).append("') = ?\n");
                params.add(entry.getValue());
            }
        }

        void applyMinScore(Double minScore) {
            if (minScore == null) return;
            sql.append("WHERE (((1.0 - cosine_distance) + 1.0) / 2.0) >= ?\n");
            params.add(minScore);
        }

        void applyPagination(int limit, int offset) {
            sql.append("ORDER BY cosine_distance ASC\n");
            sql.append("LIMIT ? OFFSET ?\n");
            params.add(limit);
            params.add(offset);
        }

        String sql() {
            return sql.toString();
        }

        Object[] params() {
            return params.toArray();
        }
    }
}

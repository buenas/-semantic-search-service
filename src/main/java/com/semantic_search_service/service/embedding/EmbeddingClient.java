package com.semantic_search_service.service.embedding;

public interface EmbeddingClient {
    float[] embed(String text);
}

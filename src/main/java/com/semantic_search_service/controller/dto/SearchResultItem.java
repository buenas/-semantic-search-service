package com.semantic_search_service.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class SearchResultItem {
    private Long id;
    private String title;
    private String content;
    private JsonNode metadata;
    private double cosineDistance;
    private double cosineSimilarity;
    private double score;

    public SearchResultItem(Long id,
                            String title,
                            String content,
                            JsonNode metadata,
                            double cosineDistance,
                            double cosineSimilarity,
                            double score) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.metadata = metadata;
        this.cosineDistance = cosineDistance;
        this.cosineSimilarity = cosineSimilarity;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public double getCosineDistance() {
        return cosineDistance;
    }

    public void setCosineDistance(double cosineDistance) {
        this.cosineDistance = cosineDistance;
    }

    public double getCosineSimilarity() {
        return cosineSimilarity;
    }

    public void setCosineSimilarity(double cosineSimilarity) {
        this.cosineSimilarity = cosineSimilarity;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}

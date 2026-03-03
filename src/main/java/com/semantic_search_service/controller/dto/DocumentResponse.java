package com.semantic_search_service.controller.dto;


import com.fasterxml.jackson.databind.JsonNode;

public class DocumentResponse {
    private Long id;
    private String title;
    private String content;
    private JsonNode metadata;

    public DocumentResponse(Long id, String title, String content, JsonNode metadata) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.metadata = metadata;
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
}

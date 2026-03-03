package com.semantic_search_service.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.semantic_search_service.controller.validation.JsonValidator;
import jakarta.validation.constraints.NotBlank;

public class UpdateDocumentRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @JsonValidator
    private JsonNode metadata;

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

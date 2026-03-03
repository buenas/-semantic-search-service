package com.semantic_search_service.controller.dto;

import com.semantic_search_service.domain.DocumentStatus;

public class CreateDocumentResponse {

    private Long id;
    private final DocumentStatus status;

    public CreateDocumentResponse(Long id, DocumentStatus status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public DocumentStatus getStatus() {
        return status;
    }

}

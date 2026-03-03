package com.semantic_search_service.service;
import com.semantic_search_service.controller.dto.*;


public interface DocumentService {

    CreateDocumentResponse create(CreateDocumentRequest createDocumentRequest);
    DocumentResponse getById(Long id);
    SearchResponse search(SearchRequest request);
    DocumentResponse update(Long id, UpdateDocumentRequest request);
    void delete(Long id);
}

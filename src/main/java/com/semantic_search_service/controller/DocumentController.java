package com.semantic_search_service.controller;

import com.semantic_search_service.controller.dto.CreateDocumentRequest;
import com.semantic_search_service.controller.dto.CreateDocumentResponse;
import com.semantic_search_service.controller.dto.DocumentResponse;
import com.semantic_search_service.controller.dto.UpdateDocumentRequest;
import com.semantic_search_service.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<CreateDocumentResponse> createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        CreateDocumentResponse response = documentService.create(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateDocumentRequest request) {
        return ResponseEntity.ok(documentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }


}

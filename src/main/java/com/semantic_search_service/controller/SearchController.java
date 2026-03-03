package com.semantic_search_service.controller;

import com.semantic_search_service.controller.dto.SearchRequest;
import com.semantic_search_service.controller.dto.SearchResponse;
import com.semantic_search_service.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final DocumentService documentService;

    public SearchController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return documentService.search(request);

    }
}

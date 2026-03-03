package com.semantic_search_service.controller.dto;

import java.util.List;

public class SearchResponse {
    private final int page;
    private final int size;
    private final int totalElements;
    private final List<SearchResultItem> items;

    public SearchResponse(int page, int size, int totalElements, List<SearchResultItem> items) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public List<SearchResultItem> getItems() {
        return items;
    }
}

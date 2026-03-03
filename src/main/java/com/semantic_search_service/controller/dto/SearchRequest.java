package com.semantic_search_service.controller.dto;

import jakarta.validation.constraints.*;

import java.util.Map;


public class SearchRequest {

    @NotBlank
    private String query;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double minScore;

    private Map<String, String> filters;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }
}

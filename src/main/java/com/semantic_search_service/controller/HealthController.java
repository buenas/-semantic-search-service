package com.semantic_search_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping(value="/ping")
    public String ping(){
        return "Ok";
    }
}

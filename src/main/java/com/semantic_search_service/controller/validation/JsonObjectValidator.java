package com.semantic_search_service.controller.validation;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JsonObjectValidator implements ConstraintValidator<JsonValidator, JsonNode> {

    @Override
    public boolean isValid(JsonNode jsonNode, ConstraintValidatorContext context) {
        return jsonNode == null || jsonNode.isObject();
    }
}

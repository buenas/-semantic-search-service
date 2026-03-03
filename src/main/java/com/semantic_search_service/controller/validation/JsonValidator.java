package com.semantic_search_service.controller.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;


@Documented
@Constraint(validatedBy = JsonObjectValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonValidator {
    String message() default "metadata must be JSON Object";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}

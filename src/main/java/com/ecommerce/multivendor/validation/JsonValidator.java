package com.ecommerce.multivendor.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JsonValidator implements ConstraintValidator<ValidJson, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidJson.JsonType requiredType;

    @Override
    public void initialize(ValidJson constraintAnnotation) {
        this.requiredType = constraintAnnotation.type();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // Blank/null is always valid here — ProductService normalizes it to
        // "[]" / "{}" before it ever reaches the database.
        if (value == null || value.isBlank()) {
            return true;
        }

        JsonNode node;
        try {
            node = MAPPER.readTree(value);
        } catch (Exception ex) {
            return fail(ctx, "Must be valid JSON syntax");
        }

        boolean matchesType = switch (requiredType) {
            case ARRAY -> node.isArray();
            case OBJECT -> node.isObject();
        };

        if (!matchesType) {
            String expected = requiredType == ValidJson.JsonType.ARRAY ? "a JSON array, e.g. [\"tag1\",\"tag2\"]"
                    : "a JSON object, e.g. {\"key\":\"value\"}";
            return fail(ctx, "Must be " + expected);
        }

        return true;
    }

    private boolean fail(ConstraintValidatorContext ctx, String message) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
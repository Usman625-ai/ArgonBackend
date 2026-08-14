package com.ecommerce.multivendor.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Field-level constraint: the annotated String must be either null/blank
 * OR syntactically valid JSON matching the required root type.
 *
 * Applied to ProductRequest.tags (must be a JSON array, e.g. ["red","summer"])
 * and ProductRequest.specifications (must be a JSON object, e.g. {"color":"Red"}).
 *
 * These fields are persisted into MySQL JSON columns (see Product entity).
 * MySQL rejects anything that isn't valid JSON at the SQL level with an
 * opaque "Data truncation: Invalid JSON text" error — this constraint
 * catches that earlier and turns it into a clean 400 validation error
 * instead of a raw DB integrity exception.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = JsonValidator.class)
@Documented
public @interface ValidJson {

    JsonType type();

    String message() default "Must be valid JSON";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    enum JsonType { ARRAY, OBJECT }
}
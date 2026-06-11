package com.ecommerce.multivendor.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Cross-field constraint: discountedPrice must be strictly less than price.
 * Applied at class level on ProductRequest.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProductPriceValidator.class)
@Documented
public @interface ValidProductPrice {
    String message() default "Discounted price must be less than the original price";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
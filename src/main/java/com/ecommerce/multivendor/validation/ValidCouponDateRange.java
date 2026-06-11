package com.ecommerce.multivendor.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Cross-field constraint: coupon validFrom must be strictly before validUntil.
 * Applied at class level on CouponRequest.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CouponDateRangeValidator.class)
@Documented
public @interface ValidCouponDateRange {
    String message() default "Coupon end date must be after the start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

package com.ecommerce.multivendor.validation;

import com.ecommerce.multivendor.dto.request.CouponRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class CouponDateRangeValidator
        implements ConstraintValidator<ValidCouponDateRange, CouponRequest> {

    @Override
    public boolean isValid(CouponRequest req, ConstraintValidatorContext ctx) {
        if (req == null) return true;

        LocalDate from = req.getValidFrom();
        LocalDate to   = req.getValidUntil();

        if (from == null || to == null) return true; // individual @NotNull catches these

        if (!to.isAfter(from)) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                            "Coupon end date must be after start date")
                    .addPropertyNode("validUntil")
                    .addConstraintViolation();
            return false;
        }

        // Minimum duration: 1 hour
        if (to.isBefore(from.plusDays(1))) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                            "Coupon must be valid for at least 1 day")
                    .addPropertyNode("validUntil")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

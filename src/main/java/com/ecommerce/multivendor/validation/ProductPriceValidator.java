package com.ecommerce.multivendor.validation;

import com.ecommerce.multivendor.dto.request.ProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * Enforces: if discountedPrice is provided, it must be < price.
 * Also ensures discountedPrice is positive.
 */
public class ProductPriceValidator
        implements ConstraintValidator<ValidProductPrice, ProductRequest> {

    @Override
    public boolean isValid(ProductRequest req, ConstraintValidatorContext ctx) {
        if (req == null) return true;

        BigDecimal price     = req.getPrice();
        BigDecimal discPrice = req.getDiscountedPrice();

        // No discounted price provided — always valid
        if (discPrice == null) return true;

        // Discounted price must be positive
        if (discPrice.compareTo(BigDecimal.ZERO) <= 0) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("Discounted price must be greater than 0")
                    .addPropertyNode("discountedPrice")
                    .addConstraintViolation();
            return false;
        }

        // Discounted price must be strictly less than the original price
        if (price != null && discPrice.compareTo(price) >= 0) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                            "Discounted price (" + discPrice + ") must be less than original price (" + price + ")")
                    .addPropertyNode("discountedPrice")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

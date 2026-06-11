package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidateRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 30)
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Invalid coupon code format")
    private String couponCode;

    @NotNull(message = "Order amount is required")
    @DecimalMin(value = "0.01", message = "Order amount must be positive")
    private BigDecimal orderAmount;
}

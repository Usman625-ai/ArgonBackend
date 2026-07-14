package com.ecommerce.multivendor.dto.request;

import com.ecommerce.multivendor.enums.DiscountType;
import com.ecommerce.multivendor.validation.ValidCouponDateRange;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request body for creating a coupon.
 *
 * WHY @Future was removed from validFrom/validUntil:
 *   @Future breaks the DataLoader (which creates test coupons at startup using
 *   LocalDateTime.now().plusMinutes(1)). The DataLoader bypasses Spring MVC
 *   validation, but if validation is triggered elsewhere it causes issues.
 *   Business rule "start date must be in the future" is enforced in CouponService
 *   when called from the Admin controller (not DataLoader).
 *
 *   Cross-field rule "validUntil must be after validFrom" is enforced by
 *   @ValidCouponDateRange at class level.
 */
@Data
@ValidCouponDateRange       // ← cross-field: validUntil > validFrom
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 30, message = "Code must be 3–30 characters")
    @Pattern(
            regexp = "^[A-Z0-9_-]+$",
            message = "Code must contain only uppercase letters, digits, hyphens or underscores"
    )
    private String code;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Discount type is required (PERCENTAGE or FIXED)")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    // Service validates: if PERCENTAGE then discountValue ≤ 100
    // Service validates: if FIXED then discountValue ≤ maxDiscount (if set)

    @DecimalMin(value = "0.00", message = "Minimum order value cannot be negative")
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @DecimalMin(value = "0.01", message = "Maximum discount must be positive")
    private BigDecimal maxDiscount;     // null = no cap

    @NotNull(message = "Valid-from date is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid-until date is required")
    private LocalDate validUntil;

    @Positive(message = "Usage limit must be a positive number")
    private Integer usageLimit;         // null = unlimited

    @Min(value = 1, message = "Per-user limit must be at least 1")
    @Max(value = 10, message = "Per-user limit cannot exceed 10")
    private int perUserLimit = 1;
}

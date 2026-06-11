package com.ecommerce.multivendor.entity;

import com.ecommerce.multivendor.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupon_code", columnList = "code")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 30, message = "Code must be 3-30 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must be uppercase letters, digits, hyphens or underscores")
    @Column(nullable = false, unique = true)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be positive")
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;  // Cap on percentage discounts

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "usage_limit")
    private Integer usageLimit;  // null = unlimited

    @Column(name = "used_count")
    @Builder.Default
    private int usedCount = 0;

    @Column(name = "per_user_limit")
    @Builder.Default
    private int perUserLimit = 1;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active
            && now.isAfter(validFrom)
            && now.isBefore(validUntil)
            && (usageLimit == null || usedCount < usageLimit);
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount.compareTo(minOrderValue) < 0) return BigDecimal.ZERO;
        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100));
            if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
                discount = maxDiscount;
            }
        } else {
            discount = discountValue;
        }
        return discount.min(orderAmount);
    }
}

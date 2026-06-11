package com.ecommerce.multivendor.dto.response;

import com.ecommerce.multivendor.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class CouponResponse {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usageLimit;
    private int usedCount;
    private boolean active;
    private boolean valid;
    // Only when validating
    private BigDecimal applicableDiscount;
}

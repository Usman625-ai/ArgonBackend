package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal unitPrice;
    private BigDecimal effectivePrice;
    private int quantity;
    private BigDecimal itemTotal;
    private int availableStock;
    private boolean inStock;
}

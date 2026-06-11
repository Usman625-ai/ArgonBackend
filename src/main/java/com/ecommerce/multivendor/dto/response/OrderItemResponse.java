package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}

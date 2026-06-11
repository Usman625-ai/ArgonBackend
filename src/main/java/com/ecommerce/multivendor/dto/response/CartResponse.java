package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class CartResponse {
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String appliedCoupon;
}

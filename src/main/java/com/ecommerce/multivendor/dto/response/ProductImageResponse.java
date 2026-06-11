package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ProductImageResponse {
    private Long id;
    private String imageUrl;
    private boolean primary;
    private int displayOrder;
}

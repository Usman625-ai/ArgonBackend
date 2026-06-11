package com.ecommerce.multivendor.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private BigDecimal effectivePrice;
    private int stockQuantity;
    private boolean inStock;
    private String brand;
    private String tags;
    private String specifications;
    private BigDecimal averageRating;
    private int totalReviews;
    private int totalSold;
    private boolean active;
    private boolean featured;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;
    private String shopName;
    private List<ProductImageResponse> images;
    private String primaryImageUrl;
    private LocalDateTime createdAt;
}

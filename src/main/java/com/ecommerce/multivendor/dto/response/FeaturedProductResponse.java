package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FeaturedProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String currencySymbol;
    private int stockQuantity;
    private String primaryImageUrl;
    private List<ProductImageResponse> images;
    private Double averageRating;
    private int reviewCount;
    private String categoryName;
    private String sellerShopName;
    private LocalDateTime featuredAt;
}
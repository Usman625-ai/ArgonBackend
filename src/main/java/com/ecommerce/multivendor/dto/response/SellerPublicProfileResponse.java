package com.ecommerce.multivendor.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Everything about a seller that's safe and useful to show a customer on a
 * public storefront page — deliberately excludes anything internal
 * (email, GST/PAN numbers, bank details, contact number, raw sellerStatus).
 */
@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerPublicProfileResponse {
    private Long id;
    private String name;
    private String shopName;
    private String shopDescription;
    private String shopLogo;
    private String shopBanner;
    private LocalDateTime memberSince;
    private long totalProducts;
    private long ordersDelivered;
    private Double averageRating;
    private long totalReviews;
}
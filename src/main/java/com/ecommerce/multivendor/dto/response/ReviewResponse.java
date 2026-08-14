package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImage;
    private Long userId;
    private String userName;
    private String userProfileImage;
    private int rating;
    private String comment;
    private boolean verifiedPurchase;
    private int helpfulCount;
    private LocalDateTime createdAt;
}
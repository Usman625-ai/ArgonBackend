package com.ecommerce.multivendor.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Public reviewer profile — shown when another shopper clicks a reviewer's
 * name/avatar on a product page. Deliberately excludes email, phone,
 * addresses, order history, and anything else non-review-related.
 */
@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerPublicProfileResponse {
    private Long id;
    private String name;
    private String profileImage;
    private LocalDateTime memberSince;
    private long totalReviews;
}
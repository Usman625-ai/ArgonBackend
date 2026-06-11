package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1 star")
    @Max(value = 5, message = "Rating cannot exceed 5 stars")
    private Integer rating;

    @Size(min = 5, max = 2000, message = "Review comment must be 5-2000 characters")
    private String comment;
}

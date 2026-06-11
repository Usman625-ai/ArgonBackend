package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CartRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Cannot add more than 100 of the same item")
    private int quantity = 1;
}

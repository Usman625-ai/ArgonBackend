package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockUpdateRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Max(value = 999999, message = "Stock quantity too large")
    private Integer quantity;

    @Size(max = 200, message = "Reason max 200 characters")
    private String reason;
}

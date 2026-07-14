package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductStatusUpdateRequest {
    @NotNull(message = "Active status is required")
    private Boolean active;
}
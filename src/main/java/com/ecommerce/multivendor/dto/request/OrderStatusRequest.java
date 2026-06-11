package com.ecommerce.multivendor.dto.request;

import com.ecommerce.multivendor.enums.OrderStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderStatusRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    @Size(max = 500, message = "Comment max 500 characters")
    private String comment;

    @Size(max = 100, message = "Tracking number max 100 characters")
    private String trackingNumber;
}

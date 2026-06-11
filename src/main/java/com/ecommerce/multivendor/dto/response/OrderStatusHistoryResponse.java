package com.ecommerce.multivendor.dto.response;

import com.ecommerce.multivendor.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class OrderStatusHistoryResponse {
    private OrderStatus status;
    private String comment;
    private String updatedBy;
    private LocalDateTime createdAt;
}

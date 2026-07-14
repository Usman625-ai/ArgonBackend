package com.ecommerce.multivendor.dto.response;

import com.ecommerce.multivendor.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderStatusHistoryResponse {
    private Long id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String notes;
    private String changedByName;
    private String changedByRole;
    private LocalDateTime createdAt;
    private String formattedDate;
    private String statusLabel;
    private String statusDescription;
}
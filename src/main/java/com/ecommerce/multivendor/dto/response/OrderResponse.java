package com.ecommerce.multivendor.dto.response;

import com.ecommerce.multivendor.enums.OrderStatus;
import com.ecommerce.multivendor.enums.PaymentMethod;
import com.ecommerce.multivendor.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private Long sellerId;
    private String sellerName;
    private String shopName;
    private List<OrderItemResponse> orderItems;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal finalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private String shippingAddress;
    private String trackingNumber;
    private String couponCode;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime deliveredAt;
    private String cancellationReason;
    private List<OrderStatusHistoryResponse> statusHistory;
    private boolean cancellable;
    private LocalDateTime createdAt;
}

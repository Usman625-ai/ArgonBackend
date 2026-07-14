package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.response.ApiResponse;
import com.ecommerce.multivendor.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.multivendor.entity.Order;
import com.ecommerce.multivendor.entity.OrderStatusHistory;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.enums.OrderStatus;
import com.ecommerce.multivendor.repository.OrderRepository;
import com.ecommerce.multivendor.repository.OrderStatusHistoryRepository;
import com.ecommerce.multivendor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepository historyRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH);

    @Transactional(readOnly = true)
    public ApiResponse<List<OrderStatusHistoryResponse>> getOrderStatusHistory(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        User currentUser = getCurrentUser();
        if (!isOrderAuthorized(order, currentUser)) {
            throw new RuntimeException("You are not authorized to view this order's status history");
        }

        List<OrderStatusHistory> histories = historyRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        List<OrderStatusHistoryResponse> response = new ArrayList<>();

        for (OrderStatusHistory history : histories) {
            response.add(mapToResponse(history));
        }

        // Add current status as final entry
        if (histories.isEmpty() || !histories.get(histories.size() - 1).getNewStatus().equals(order.getOrderStatus())) {
            response.add(OrderStatusHistoryResponse.builder()
                    .id(null)
                    .previousStatus(order.getOrderStatus())
                    .newStatus(order.getOrderStatus())
                    .notes("Current status")
                    .changedByName("System")
                    .changedByRole("SYSTEM")
                    .createdAt(order.getUpdatedAt())
                    .formattedDate(order.getUpdatedAt() != null ? order.getUpdatedAt().format(FORMATTER) : "N/A")
                    .statusLabel(getStatusLabel(order.getOrderStatus()))
                    .statusDescription(getStatusDescription(order.getOrderStatus()))
                    .build());
        }

        return ApiResponse.success("Order status history retrieved successfully", response);
    }

    @Transactional
    public void logStatusChange(Order order, OrderStatus previousStatus, OrderStatus newStatus, String notes) {
        User currentUser = getCurrentUserOrNull();

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .notes(notes)
                .changedBy(currentUser)  // ✅ User entity, not String
                .build();

        historyRepository.save(history);
    }

    private OrderStatusHistoryResponse mapToResponse(OrderStatusHistory history) {
        User changedBy = history.getChangedBy();
        String changedByName = changedBy != null ? changedBy.getName() : "System";
        String changedByRole = changedBy != null && changedBy.getRole() != null
                ? changedBy.getRole().name()
                : "SYSTEM";

        return OrderStatusHistoryResponse.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .notes(history.getNotes())
                .changedByName(changedByName)
                .changedByRole(changedByRole)
                .createdAt(history.getCreatedAt())
                .formattedDate(history.getCreatedAt() != null ? history.getCreatedAt().format(FORMATTER) : "N/A")
                .statusLabel(getStatusLabel(history.getNewStatus()))
                .statusDescription(getStatusDescription(history.getNewStatus()))
                .build();
    }

    private boolean isOrderAuthorized(Order order, User currentUser) {
        if (currentUser == null) return false;
        return currentUser.getRole().name().equals("ADMIN") ||
                order.getCustomer().getId().equals(currentUser.getId()) ||
                (order.getSeller() != null && order.getSeller().getId().equals(currentUser.getId()));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private User getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }

    private String getStatusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Order Placed";
            case CONFIRMED -> "Confirmed";
            case PROCESSING -> "Processing";
            case SHIPPED -> "Shipped";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
            case REFUNDED -> "Refunded";
            case RETURNED -> "Returned";
        };
    }

    private String getStatusDescription(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Your order has been placed and is awaiting confirmation.";
            case CONFIRMED -> "Your order has been confirmed by the seller.";
            case PROCESSING -> "Your order is being prepared for shipment.";
            case SHIPPED -> "Your order has been shipped and is on its way.";
            case DELIVERED -> "Your order has been delivered successfully.";
            case CANCELLED -> "Your order has been cancelled.";
            case REFUNDED -> "A refund has been processed for your order.";
            case RETURNED -> "Your order has been returned.";
        };
    }
}
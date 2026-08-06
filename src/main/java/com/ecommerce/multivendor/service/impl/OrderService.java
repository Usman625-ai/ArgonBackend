package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.CheckoutRequest;
import com.ecommerce.multivendor.dto.request.OrderStatusRequest;
import com.ecommerce.multivendor.dto.request.PaymentVerifyRequest;
import com.ecommerce.multivendor.dto.response.*;
import com.ecommerce.multivendor.entity.*;
import com.ecommerce.multivendor.enums.*;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.exception.UnauthorizedException;
import com.ecommerce.multivendor.repository.*;
import com.ecommerce.multivendor.util.OrderNumberGenerator;
import com.ecommerce.multivendor.config.RetryableRead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final AddressRepository addressRepository;
    private final CartService cartService;
    private final CouponService couponService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    /** Feature flag — flip to true (app.payments.jazzcash-enabled) once JazzCash is back. */
    @Value("${app.payments.jazzcash-enabled:false}")
    private boolean jazzCashEnabled;

    // ─── Checkout ──────────────────────────────────────────────────────────

    /**
     * Generates a durable, collision-safe order number.
     * The previous implementation used a static in-memory AtomicInteger, which
     * reset to 0 on every application restart — causing it to collide with
     * order numbers already persisted earlier the same day (the exact
     * "Duplicate entry 'ORDyyyyMMdd0001'" errors seen in production).
     * This version derives the next sequence from the database itself.
     */
    private String generateOrderNumber() {
        String datePrefix = com.ecommerce.multivendor.util.AppConstants.ORDER_PREFIX
                + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = orderRepository.countByOrderNumberStartingWith(datePrefix) + 1;
        String candidate = String.format("%s%04d", datePrefix, seq);
        int guard = 0;
        while (orderRepository.findByOrderNumber(candidate).isPresent() && guard < 50) {
            seq++;
            candidate = String.format("%s%04d", datePrefix, seq);
            guard++;
        }
        return candidate;
    }

    /**
     * Place an order from cart.
     * - Groups cart items by seller (each seller = one Order)
     * - Deducts stock
     * - Applies coupon if provided
     * - Returns list of created orders (one per seller)
     */
    public List<OrderResponse> checkout(CheckoutRequest request, User customer) {
        if (request.getPaymentMethod() == PaymentMethod.JAZZCASH && !jazzCashEnabled) {
            throw new BadRequestException("Sorry, JazzCash payments aren't available right now. Please choose Cash on Delivery — JazzCash will be back soon!");
        }

        List<CartItem> cartItems = cartService.getCartItems(customer.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));

        if (!address.getUser().getId().equals(customer.getId())) {
            throw new UnauthorizedException("Address does not belong to you");
        }

        // Group items by seller
        Map<User, List<CartItem>> itemsBySeller = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            User seller = item.getProduct().getSeller();
            itemsBySeller.computeIfAbsent(seller, k -> new ArrayList<>()).add(item);
        }

        // Validate all stock before deducting
        for (CartItem item : cartItems) {
            Product p = item.getProduct();
            if (!p.isActive()) {
                throw new BadRequestException("Product '" + p.getName() + "' is no longer available");
            }
            if (p.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Insufficient stock for: " + p.getName()
                        + " (Available: " + p.getStockQuantity() + ")");
            }
        }

        // Calculate coupon discount across all items (applied to first order only)
        BigDecimal totalCartValue = cartItems.stream()
                .map(ci -> ci.getProduct().getEffectivePrice()
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            totalDiscount = couponService.applyCoupon(request.getCouponCode(), totalCartValue, customer.getId());
        }

        List<Order> createdOrders = new ArrayList<>();
        boolean isFirstOrder = true;

        for (Map.Entry<User, List<CartItem>> entry : itemsBySeller.entrySet()) {
            User seller = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();

            // Calculate subtotal for this seller's items
            BigDecimal subtotal = sellerItems.stream()
                    .map(ci -> ci.getProduct().getEffectivePrice()
                            .multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Apply coupon discount proportionally (simplified: full discount to first seller)
            BigDecimal discount = isFirstOrder ? totalDiscount : BigDecimal.ZERO;
            BigDecimal finalAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);

            // Create order
            Order order = Order.builder()
                    .orderNumber(generateOrderNumber())
                    .customer(customer)
                    .seller(seller)
                    .subtotalAmount(subtotal)
                    .discountAmount(discount)
                    .shippingAmount(BigDecimal.ZERO)  // Free shipping (adjust as needed)
                    .taxAmount(BigDecimal.ZERO)
                    .finalAmount(finalAmount)
                    .paymentMethod(request.getPaymentMethod())
                    .paymentStatus(PaymentStatus.PENDING)
                    .orderStatus(OrderStatus.PENDING)
                    .shippingName(address.getFullName())
                    .shippingPhone(address.getPhoneNumber())
                    .shippingAddress(address.getFullAddress())
                    .couponCode(isFirstOrder ? request.getCouponCode() : null)
                    // estimatedDeliveryDate is intentionally left unset here — it is populated
                    // once the order is CONFIRMED (see setEstimatedDeliveryIfAbsent), so customers
                    // only see a delivery estimate after the order is actually confirmed.
                    .build();

            order = orderRepository.save(order);

            // Add order items and deduct stock
            for (CartItem ci : sellerItems) {
                Product product = ci.getProduct();
                String primaryImage = product.getImages().stream()
                        .filter(ProductImage::isPrimary).map(ProductImage::getImageUrl)
                        .findFirst().orElse(null);

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .productName(product.getName())  // snapshot
                        .productImage(primaryImage)
                        .quantity(ci.getQuantity())
                        .unitPrice(product.getEffectivePrice())
                        .totalPrice(product.getEffectivePrice()
                                .multiply(BigDecimal.valueOf(ci.getQuantity())))
                        .build();
                orderItemRepository.save(orderItem);

                // Deduct stock
                product.decreaseStock(ci.getQuantity());
            }
            // Record initial status
            recordStatusChange(order,order.getOrderStatus() , OrderStatus.PENDING, "Order placed", customer.getName());
            createdOrders.add(order);
            isFirstOrder = false;
        }

        // Clear cart
        cartService.clearCart(customer.getId());

        // Async emails + notifications
        createdOrders.forEach(order -> {
            emailService.sendOrderConfirmation(order);
            emailService.sendNewOrderNotificationToSeller(order);
            notificationService.createNotification(customer,
                    "Your order #" + order.getOrderNumber() + " has been placed successfully.",
                    "Order Placed", NotificationType.ORDER_PLACED,
                    "/orders/" + order.getId());
            notificationService.createNotification(order.getSeller(),
                    "New order #" + order.getOrderNumber() + " received.",
                    "New Order", NotificationType.ORDER_PLACED,
                    "/seller/orders/" + order.getId());
        });

        log.info("Checkout complete for customer {}. {} order(s) created.",
                customer.getId(), createdOrders.size());

        return createdOrders.stream().map(this::toOrderResponse).toList();
    }

    // ─── Payment ───────────────────────────────────────────────────────────

    public PaymentOrderResponse initiatePayment(Long orderId, Long customerId) {
        Order order = getOrderForCustomer(orderId, customerId);

        if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            throw new BadRequestException("This order uses Cash on Delivery — no online payment needed");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }

        PaymentOrderResponse paymentResponse = paymentService.createPayment(order);

        // Persist JazzCash txn reference for later hash verification
        order.setJazzCashTxnRefNo(paymentResponse.getTxnRefNo());
        orderRepository.save(order);

        return paymentResponse;
    }

    /**
     * Called after the frontend verifies payment manually,
     * OR after the JazzCash callback hits our server.
     * Uses JazzCash secure hash verification.
     */
    public OrderResponse verifyAndConfirmPayment(Long orderId, PaymentVerifyRequest request,
                                                 Long customerId) {
        Order order = getOrderForCustomer(orderId, customerId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Payment already confirmed");
        }

        com.ecommerce.multivendor.dto.response.JazzCashCallbackResponse result =
                paymentService.verifyCallback(request);

        if (!result.isSuccess()) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new BadRequestException("Payment verification failed: " + result.getResponseMessage());
        }

        OrderStatus previousStatus = order.getOrderStatus();
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentId(result.getTransactionId());   // JazzCash pp_TransactionId
        order.setOrderStatus(OrderStatus.CONFIRMED);
        setEstimatedDeliveryIfAbsent(order);
        orderRepository.save(order);

        recordStatusChange(order, previousStatus, OrderStatus.CONFIRMED,
                "JazzCash payment received. Order confirmed.", "System");

        emailService.sendPaymentSuccess(order);
        notificationService.createNotification(order.getCustomer(),
                "Payment confirmed for order #" + order.getOrderNumber(),
                "Payment Successful", NotificationType.PAYMENT_SUCCESS,
                "/orders/" + order.getId());

        log.info("JazzCash payment confirmed for order: {}", order.getOrderNumber());
        return toOrderResponse(order);
    }

    /**
     * Called directly by PaymentCallbackController when JazzCash POSTs to return URL.
     * Does NOT require customerId — identified by pp_BillReference (orderNumber).
     */
    public void processJazzCashCallback(
            com.ecommerce.multivendor.dto.response.JazzCashCallbackResponse result) {
        if (result.getOrderNumber() == null || result.getOrderNumber().isBlank()) {
            log.warn("[JazzCash Callback] Missing order number in callback");
            return;
        }

        Order order = orderRepository.findByOrderNumber(result.getOrderNumber())
                .orElse(null);
        OrderStatus previousStatus = order != null ? order.getOrderStatus() : null;
        if (order == null) {
            log.warn("[JazzCash Callback] Order not found: {}", result.getOrderNumber());
            return;
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("[JazzCash Callback] Order already paid: {}", result.getOrderNumber());
            return;
        }

        if (result.isSuccess()) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentId(result.getTransactionId());
            order.setOrderStatus(OrderStatus.CONFIRMED);
            setEstimatedDeliveryIfAbsent(order);
            orderRepository.save(order);
            recordStatusChange(order,previousStatus ,OrderStatus.CONFIRMED,
                    "JazzCash payment received via callback.", "System");
            emailService.sendPaymentSuccess(order);
            notificationService.createNotification(order.getCustomer(),
                    "Payment confirmed for order #" + order.getOrderNumber(),
                    "Payment Successful", NotificationType.PAYMENT_SUCCESS,
                    "/orders/" + order.getId());
            log.info("[JazzCash Callback] Payment marked PAID for order: {}",
                    order.getOrderNumber());
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            log.warn("[JazzCash Callback] Payment FAILED for order: {} | Reason: {}",
                    order.getOrderNumber(), result.getResponseMessage());
        }
    }

    // ─── Customer: Orders ──────────────────────────────────────────────────

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getCustomerOrders(Long customerId, int page, int size) {
        return getCustomerOrders(customerId, page, size, null);
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getCustomerOrders(Long customerId, int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = status != null
                ? orderRepository.findByCustomerIdAndOrderStatusOrderByCreatedAtDesc(customerId, status, pageable)
                : orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        return toPagedResponse(orderPage);
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrder(Long orderId, Long customerId) {
        return toOrderResponse(getOrderForCustomer(orderId, customerId));
    }

    public OrderResponse cancelOrder(Long orderId, Long customerId, String reason) {
        Order order = getOrderForCustomer(orderId, customerId);

        OrderStatus previousStatus = order.getOrderStatus();

        if (!order.isCancellable()) {
            throw new BadRequestException(
                    "Order cannot be cancelled. Only PENDING or CONFIRMED orders can be cancelled.");
        }

        // Restore stock
        order.getOrderItems().forEach(item ->
                item.getProduct().increaseStock(item.getQuantity())
        );

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        // Refund online payment if already paid
        if (order.getPaymentStatus() == PaymentStatus.PAID
                && order.getJazzCashTxnRefNo() != null) {
            // Pass the original pp_TxnRefNo (jazzCashTxnRefNo) — NOT paymentId
            // JazzCash refund API needs pp_TxnRefNoToBeRefunded = original pp_TxnRefNo
            paymentService.initiateRefund(order.getJazzCashTxnRefNo(), order.getFinalAmount());
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        orderRepository.save(order);
        recordStatusChange(order,previousStatus ,OrderStatus.CANCELLED,
                reason != null ? reason : "Cancelled by customer", order.getCustomer().getName());

        emailService.sendOrderCancelled(order, reason);
        notificationService.createNotification(order.getCustomer(),
                "Your order #" + order.getOrderNumber() + " has been cancelled.",
                "Order Cancelled", NotificationType.ORDER_CANCELLED,
                "/orders/" + order.getId());

        log.info("Order {} cancelled by customer {}", orderId, customerId);
        return toOrderResponse(order);
    }

    // ─── Seller: Orders ────────────────────────────────────────────────────

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getSellerOrders(Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository
                .findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
        return toPagedResponse(orderPage);
    }

    public OrderResponse updateOrderStatus(Long orderId, OrderStatusRequest request, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("You are not authorized to update this order");
        }

        // Validate status transition
        validateStatusTransition(order.getOrderStatus(), request.getStatus());

        order.setOrderStatus(request.getStatus());

        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getStatus() == OrderStatus.CONFIRMED) {
            setEstimatedDeliveryIfAbsent(order);
        }
        if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            // Auto-confirm payment for COD
            if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        }

        OrderStatus previousStatus = order.getOrderStatus();
        orderRepository.save(order);
        recordStatusChange(order, previousStatus, request.getStatus(), request.getComment(), order.getSeller().getName());


        // Email & notification
        if (request.getStatus() == OrderStatus.SHIPPED) {
            emailService.sendOrderShipped(order);
            notificationService.createNotification(order.getCustomer(),
                    "Your order #" + order.getOrderNumber() + " has been shipped!",
                    "Order Shipped", NotificationType.ORDER_SHIPPED,
                    "/orders/" + order.getId());
        } else if (request.getStatus() == OrderStatus.DELIVERED) {
            emailService.sendOrderDelivered(order);
            notificationService.createNotification(order.getCustomer(),
                    "Your order #" + order.getOrderNumber() + " has been delivered!",
                    "Order Delivered", NotificationType.ORDER_DELIVERED,
                    "/orders/" + order.getId());
        }

        log.info("Order {} status updated to {} by seller {}",
                orderId, request.getStatus(), sellerId);
        return toOrderResponse(order);
    }

    // ─── Admin: Orders ─────────────────────────────────────────────────────

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toPagedResponse(orderPage);
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return toOrderResponse(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId)));
    }

    // ─── Auto-cancel (called by Scheduler) ───────────────────────────────

    /**
     * Auto-cancelled orders are unpaid JazzCash orders whose payment window has
     * expired (see OrderRepository#findPendingOrdersOlderThan — COD orders are
     * excluded and never auto-cancelled). These were never actually paid for, so —
     * unlike customer-initiated cancellations, which are kept as history — they are
     * removed from the database entirely (order, its items, and its status history)
     * rather than just being marked CANCELLED. Stock is restored and the customer is
     * notified beforehand.
     */
    public void autoCancelPendingOrders(LocalDateTime cutoff) {
        List<Order> pendingOrders = orderRepository.findPendingOrdersOlderThan(cutoff);
        pendingOrders.forEach(order -> {
            order.getOrderItems().forEach(item ->
                    item.getProduct().increaseStock(item.getQuantity()));

            String orderNumber = order.getOrderNumber();
            emailService.sendOrderCancelled(order, "Auto-cancelled: JazzCash payment not received within the payment window");
            notificationService.createNotification(order.getCustomer(),
                    "Your order #" + orderNumber + " was automatically cancelled because JazzCash payment wasn't completed in time.",
                    "Order Cancelled", NotificationType.ORDER_CANCELLED, "/shop/orders");

            orderRepository.delete(order);
            log.info("Auto-cancelled order {} removed from database", orderNumber);
        });
        log.info("Auto-cancelled {} pending orders", pendingOrders.size());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /** Sets the estimated delivery date the first time an order is confirmed (idempotent). */
    private static final int ESTIMATED_DELIVERY_DAYS = 5;

    private void setEstimatedDeliveryIfAbsent(Order order) {
        if (order.getEstimatedDeliveryDate() == null) {
            order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(ESTIMATED_DELIVERY_DAYS));
        }
    }

    private Order getOrderForCustomer(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedException("You are not authorized to access this order");
        }
        return order;
    }

    private void recordStatusChange(Order order, OrderStatus previousStatus, OrderStatus newStatus,
                                    String comment, String updatedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)  // ✅ Now tracking previous status
                .newStatus(newStatus)
                .notes(comment)
                .changedBy(null)  // Set to null or fetch actual user if needed
                .build();
        statusHistoryRepository.save(history);
    }

    /**
     * Enforce allowed status transitions to prevent invalid state jumps.
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        Map<OrderStatus, List<OrderStatus>> allowed = Map.of(
                OrderStatus.PENDING, List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
                OrderStatus.CONFIRMED, List.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
                OrderStatus.PROCESSING, List.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
                OrderStatus.SHIPPED, List.of(OrderStatus.DELIVERED),
                OrderStatus.DELIVERED, List.of(OrderStatus.REFUNDED,OrderStatus.RETURNED,OrderStatus.DELIVERED)
        );

        List<OrderStatus> validTransitions = allowed.getOrDefault(current, List.of());
        if (!validTransitions.contains(next)) {
            throw new BadRequestException(
                    "Cannot transition order from " + current + " to " + next);
        }
    }

    // ─── Mappers ───────────────────────────────────────────────────────────

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderStatusHistory> history = statusHistoryRepository
                .findByOrderIdOrderByCreatedAtAsc(order.getId());
        return buildOrderResponse(order, items, history);
    }

    private OrderResponse buildOrderResponse(Order order, List<OrderItem> items, List<OrderStatusHistory> history) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .sellerId(order.getSeller().getId())
                .sellerName(order.getSeller().getName())
                .shopName(order.getSeller().getShopName())
                .orderItems(items.stream().map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .productImage(item.getProductImage())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build()).toList())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingAmount(order.getShippingAmount())
                .taxAmount(order.getTaxAmount())
                .finalAmount(order.getFinalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .shippingAddress(order.getShippingAddress())
                .trackingNumber(order.getTrackingNumber())
                .couponCode(order.getCouponCode())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .deliveredAt(order.getDeliveredAt())
                .cancellationReason(order.getCancellationReason())
                .cancellable(order.isCancellable())
                .statusHistory(history.stream().map(h -> OrderStatusHistoryResponse.builder()
                        .newStatus(h.getNewStatus())
                        .notes(h.getNotes())
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getName() : "System")
                        .createdAt(h.getCreatedAt())
                        .build()).toList())
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * Maps a page of orders in bulk: ONE query for every order's items and ONE
     * query for every order's status history (instead of two queries per order —
     * the same N+1 pattern fixed for product listings, and just as costly against
     * a non-local database).
     */
    private PagedResponse<OrderResponse> toPagedResponse(Page<Order> page) {
        List<Order> orders = page.getContent();
        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        Map<Long, List<OrderItem>> itemsByOrder = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(i -> i.getOrder().getId()));

        Map<Long, List<OrderStatusHistory>> historyByOrder = orderIds.isEmpty()
                ? Map.of()
                : statusHistoryRepository.findByOrderIdInOrderByCreatedAtAsc(orderIds).stream()
                .collect(Collectors.groupingBy(h -> h.getOrder().getId()));

        List<OrderResponse> content = orders.stream()
                .map(o -> buildOrderResponse(o,
                        itemsByOrder.getOrDefault(o.getId(), List.of()),
                        historyByOrder.getOrDefault(o.getId(), List.of())))
                .toList();

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
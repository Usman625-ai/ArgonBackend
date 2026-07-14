package com.ecommerce.multivendor.controller;

import com.ecommerce.multivendor.dto.request.*;
import com.ecommerce.multivendor.dto.response.*;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.repository.UserRepository;
import com.ecommerce.multivendor.security.SecurityUtils;
import com.ecommerce.multivendor.service.impl.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final OrderStatusHistoryService historyService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final WishlistService wishlistService;
    private final AddressService addressService;
    private final CouponService couponService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    // ─── Products (read-only, no auth needed but included for personalization) ──

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> browseProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            productService.searchProducts(q, categoryId, minPrice, maxPrice,
                brand, sortBy, sortDir, page, size)
        ));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductByIdPublic(id)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<FeaturedProductResponse>>> getFeaturedProducts(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(productService.getFeaturedProducts(limit));
    }

    // Cart

    @PostMapping("/cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartRequest request) {
        User customer = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
            "Added to cart", cartService.addToCart(customer, request)
        ));
    }

    @GetMapping("/cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success(
            cartService.getCart(securityUtils.getCurrentUserId())
        ));
    }

    @PutMapping("/cart/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCart(
            @PathVariable Long itemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cart updated",
            cartService.updateCartItem(itemId, quantity, securityUtils.getCurrentUserId())
        ));
    }

    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(@PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Item removed",
            cartService.removeFromCart(itemId, securityUtils.getCurrentUserId())
        ));
    }

    @DeleteMapping("/cart/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }


    // Orders

    //   Status

    @GetMapping("/orders/{id}/status-history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderStatusHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(historyService.getOrderStatusHistory(id));
    }

    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        User customer = securityUtils.getCurrentUser();
        List<OrderResponse> orders = orderService.checkout(request, customer);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Order placed successfully! " +
                orders.size() + " order(s) created.", orders));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            orderService.getCustomerOrders(securityUtils.getCurrentUserId(), page, size)
        ));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            orderService.getCustomerOrder(id, securityUtils.getCurrentUserId())
        ));
    }

    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", null) : null;
        return ResponseEntity.ok(ApiResponse.success(
            "Order cancelled",
            orderService.cancelOrder(id, securityUtils.getCurrentUserId(), reason)
        ));
    }

    /**
     * Step 1 — Initiate JazzCash payment.
     * Returns hostedPageUrl + signed formParams.
     * Frontend must POST those params as a hidden form to hostedPageUrl.
     */
    @PostMapping("/orders/{id}/payment/initiate")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> initiatePayment(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "JazzCash payment initiated. Submit the form to hostedPageUrl.",
            orderService.initiatePayment(id, securityUtils.getCurrentUserId())
        ));
    }

    /**
     * Step 2 — Verify payment after JazzCash redirects back to frontend.
     * Frontend extracts all pp_ params from the redirect URL and posts them here.
     * Server re-computes pp_SecureHash and marks order PAID if valid.
     */
    @PostMapping("/orders/{id}/payment/verify")
    public ResponseEntity<ApiResponse<OrderResponse>> verifyPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Payment verified and order confirmed.",
            orderService.verifyAndConfirmPayment(id, request, securityUtils.getCurrentUserId())
        ));
    }

    // Reviews

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @Valid @RequestBody ReviewRequest request) {
        User customer = securityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Review submitted", reviewService.addReview(request, customer)));
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            reviewService.getProductReviews(productId, page, size)
        ));
    }

    // Wishlist

    @GetMapping("/wishlist")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            wishlistService.getWishlist(securityUtils.getCurrentUserId(), page, size)
        ));
    }

    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(@PathVariable Long productId) {
        wishlistService.addToWishlist(productId, securityUtils.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist"));
    }

    @DeleteMapping("/wishlist/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(productId, securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist"));
    }

    // Addresses

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses() {
        return ResponseEntity.ok(ApiResponse.success(
            addressService.getAddresses(securityUtils.getCurrentUserId())
        ));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest request) {
        User customer = securityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Address added", addressService.addAddress(request, customer)));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Address updated",
            addressService.updateAddress(id, request, securityUtils.getCurrentUserId())
        ));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id, securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Address deleted"));
    }

    // Coupons

    @PostMapping("/coupons/validate")
    public ResponseEntity<ApiResponse<CouponResponse>> validateCoupon(
            @Valid @RequestBody CouponValidateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Coupon is valid", couponService.validateCoupon(request)
        ));
    }

    // Profile

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        User customer = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(UserResponse.builder()
            .id(customer.getId())
            .name(customer.getName())
            .email(customer.getEmail())
            .role(customer.getRole())
            .active(customer.isActive())
            .verified(customer.isVerified())
            .contactNumber(customer.getContactNumber())
            .profileImage(customer.getProfileImage())
            .createdAt(customer.getCreatedAt())
            .build()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        User customer = securityUtils.getCurrentUser();
        if (request.getName()          != null) customer.setName(request.getName());
        if (request.getContactNumber() != null) customer.setContactNumber(request.getContactNumber());
        if (request.getProfileImage()  != null) customer.setProfileImage(request.getProfileImage());
        userRepository.save(customer);                      // ← persisted properly
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
            UserResponse.builder()
                .id(customer.getId()).name(customer.getName())
                .email(customer.getEmail()).role(customer.getRole())
                .active(customer.isActive()).verified(customer.isVerified())
                .contactNumber(customer.getContactNumber())
                .profileImage(customer.getProfileImage())
                .createdAt(customer.getCreatedAt())
                .build()));
    }

    // Notifications

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<PagedResponse<?>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            notificationService.getNotifications(securityUtils.getCurrentUserId(), page, size)
        ));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
            notificationService.getUnreadCount(securityUtils.getCurrentUserId())
        ));
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllAsRead(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }
}

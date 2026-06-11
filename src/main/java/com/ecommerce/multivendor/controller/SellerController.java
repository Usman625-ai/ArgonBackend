package com.ecommerce.multivendor.controller;

import com.ecommerce.multivendor.dto.request.*;
import com.ecommerce.multivendor.dto.response.*;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.security.SecurityUtils;
import com.ecommerce.multivendor.service.impl.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    private final SellerService sellerService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReportService reportService;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    // ─── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getStats() {
        User seller = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(sellerService.getSellerStats(seller.getId())));
    }

    // ─── Profile ───────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        return ResponseEntity.ok(
                ApiResponse.success(sellerService.getSellerProfile(securityUtils.getCurrentUser()))
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestBody UpdateSellerProfileRequest request) {
        User seller = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated", sellerService.updateSellerProfile(seller, request)
        ));
    }

    // ─── Product Management ────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        User seller = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllSellerProducts(seller.getId(), page, size)
        ));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        User seller = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                productService.getSellerProduct(id, seller.getId())
        ));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        User seller = securityUtils.getCurrentUser();
        sellerService.requireApprovedSeller(seller);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.createProduct(request, seller)));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        User seller = securityUtils.getCurrentUser();
        sellerService.requireApprovedSeller(seller);
        return ResponseEntity.ok(ApiResponse.success(
                "Product updated", productService.updateProduct(id, request, seller.getId())
        ));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        User seller = securityUtils.getCurrentUser();
        productService.deleteProduct(id, seller.getId());
        return ResponseEntity.ok(ApiResponse.success("Product deleted"));
    }

    @PutMapping("/products/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request) {
        User seller = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "Stock updated", productService.updateStock(id, request, seller.getId())
        ));
    }

    /** Upload product images (multipart) */
    @PostMapping("/products/{id}/images")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        User seller = securityUtils.getCurrentUser();
        List<String> urls = productService.uploadProductImages(id, files, seller.getId());
        return ResponseEntity.ok(ApiResponse.success("Images uploaded", urls));
    }

    @DeleteMapping("/products/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long productId, @PathVariable Long imageId) {
        User seller = securityUtils.getCurrentUser();
        productService.deleteProductImage(productId, imageId, seller.getId());
        return ResponseEntity.ok(ApiResponse.success("Image deleted"));
    }

    // ─── Order Management ──────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User seller = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getSellerOrders(seller.getId(), page, size)
        ));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request) {
        User seller = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "Order status updated",
                orderService.updateOrderStatus(id, request, seller.getId())
        ));
    }

    // ─── Reports ───────────────────────────────────────────────────────────

    @GetMapping("/reports/sales")
    public ResponseEntity<byte[]> generateSalesReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        User seller = securityUtils.getCurrentUser();

        // Default: current month
        LocalDateTime start = from != null ? from : LocalDateTime.now().withDayOfMonth(1);
        LocalDateTime end   = to   != null ? to   : LocalDateTime.now();

        byte[] report = reportService.generateSellerSalesReport(seller.getId(), start, end);
        String filename = reportService.generateReportFilename("seller_sales");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(report);
    }

    // ─── Notifications ─────────────────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<PagedResponse<?>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User seller = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotifications(seller.getId(), page, size)
        ));
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllAsRead(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }
}
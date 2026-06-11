package com.ecommerce.multivendor.controller;

import com.ecommerce.multivendor.dto.request.CouponRequest;
import com.ecommerce.multivendor.dto.response.*;
import com.ecommerce.multivendor.enums.SellerStatus;
import com.ecommerce.multivendor.service.impl.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final CategoryService categoryService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final ReportService reportService;
    private final com.ecommerce.multivendor.repository.GlobalSettingsRepository settingsRepository;

    // ─── System Settings ───────────────────────────────────────────────────

    @GetMapping("/settings/maintenance")
    public ResponseEntity<ApiResponse<Boolean>> getMaintenanceMode() {
        boolean maintenance = settingsRepository.findByKey("MAINTENANCE_MODE")
                .map(s -> "true".equalsIgnoreCase(s.getValue()))
                .orElse(false);
        return ResponseEntity.ok(ApiResponse.success(maintenance));
    }

    @PostMapping("/settings/maintenance")
    public ResponseEntity<ApiResponse<Boolean>> toggleMaintenanceMode(@RequestParam boolean enable) {
        com.ecommerce.multivendor.entity.GlobalSettings setting = settingsRepository.findByKey("MAINTENANCE_MODE")
                .orElse(com.ecommerce.multivendor.entity.GlobalSettings.builder()
                        .key("MAINTENANCE_MODE")
                        .build());
        setting.setValue(String.valueOf(enable));
        settingsRepository.save(setting);
        return ResponseEntity.ok(ApiResponse.success("Maintenance mode " + (enable ? "enabled" : "disabled"), enable));
    }

    // ─── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    // ─── Seller Management ─────────────────────────────────────────────────

    @GetMapping("/sellers")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getSellers(
            @RequestParam(required = false) SellerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSellers(status, page, size)));
    }

    @GetMapping("/sellers/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSellerById(id)));
    }

    @PutMapping("/sellers/{id}/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approveSeller(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Seller approved successfully", adminService.approveSeller(id))
        );
    }

    @PutMapping("/sellers/{id}/reject")
    public ResponseEntity<ApiResponse<UserResponse>> rejectSeller(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Does not meet requirements");
        return ResponseEntity.ok(
                ApiResponse.success("Seller rejected", adminService.rejectSeller(id, reason))
        );
    }

    @PutMapping("/sellers/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleSellerStatus(
            @PathVariable Long id,
            @RequestParam boolean enable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Seller " + (enable ? "enabled" : "disabled"),
                adminService.toggleSellerStatus(id, enable)
        ));
    }

    // ─── Customer Management ───────────────────────────────────────────────

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getCustomers(page, size)));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enable) {
        return ResponseEntity.ok(ApiResponse.success(
                "User status updated", adminService.toggleUserStatus(id, enable)
        ));
    }

    // ─── Category Management ───────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<?>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody com.ecommerce.multivendor.dto.request.CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created", categoryService.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody com.ecommerce.multivendor.dto.request.CategoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Category updated", categoryService.updateCategory(id, request))
        );
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted"));
    }

    // ─── Order Management ──────────────────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(page, size)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    // ─── Coupon Management ─────────────────────────────────────────────────

    @PostMapping("/coupons")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created", couponService.createCoupon(request)));
    }

    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<PagedResponse<CouponResponse>>> getCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(couponService.getAllCoupons(page, size)));
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deactivated"));
    }

    // ─── Reports ───────────────────────────────────────────────────────────

    @GetMapping("/reports/sales")
    public ResponseEntity<byte[]> generateSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        byte[] reportBytes = reportService.generateAdminSalesReport(from, to);
        String filename = reportService.generateReportFilename("admin_sales");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reportBytes);
    }
}


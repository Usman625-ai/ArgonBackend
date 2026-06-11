package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Seller dashboard statistics.
 * Contains ONLY data relevant to the currently logged-in seller.
 * Never contains platform-wide figures (those belong to AdminDashboardResponse).
 */
@Data
@Builder
public class SellerDashboardResponse {

    // ── Product stats ──────────────────────────────────────────────────
    /** Total active products listed by this seller */
    private long totalProducts;

    /** Products with stock ≤ 10 (low stock alert) */
    private long lowStockProducts;

    /** Products currently out of stock */
    private long outOfStockProducts;

    // ── Order stats ────────────────────────────────────────────────────
    /** Total orders received (all time, excluding cancelled) */
    private long totalOrders;

    /** Orders waiting for seller action (PENDING or CONFIRMED) */
    private long pendingOrders;

    /** Orders currently being processed or shipped */
    private long activeOrders;

    /** Orders successfully delivered */
    private long deliveredOrders;

    /** Orders cancelled */
    private long cancelledOrders;

    // ── Revenue stats ──────────────────────────────────────────────────
    /** Total revenue from all paid orders (all time) */
    private BigDecimal totalRevenue;

    /** Revenue earned this calendar month */
    private BigDecimal monthlyRevenue;

    /** Revenue earned today */
    private BigDecimal todayRevenue;

    // ── Shop info ──────────────────────────────────────────────────────
    private String shopName;
    private String sellerStatus;

    // ── Chart data ─────────────────────────────────────────────────────
    /** Last 7 days revenue: { "2024-01-15": 12500.00, ... } */
    private Map<String, BigDecimal> dailyRevenue;

    /** Orders grouped by status: { "PENDING": 3, "SHIPPED": 7, ... } */
    private Map<String, Long> ordersByStatus;
}
package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Admin dashboard statistics — platform-wide figures.
 * Used ONLY by /api/admin/dashboard/stats.
 *
 * Never returned to sellers or customers.
 */
@Data
@Builder
public class DashboardStatsResponse {

    // ── User counts ────────────────────────────────────────────────────
    /** Total registered users (sellers + customers) */
    private long totalUsers;

    /** Total registered sellers */
    private long totalSellers;

    /** Approved (active) sellers */
    private long approvedSellers;

    /** Sellers waiting for admin approval */
    private long pendingSellerApprovals;

    /** Total registered customers */
    private long totalCustomers;

    // ── Product counts ─────────────────────────────────────────────────
    /** Total active products across all sellers */
    private long totalProducts;

    // ── Order counts ───────────────────────────────────────────────────
    /** Total orders placed (all time) */
    private long totalOrders;

    /** Orders currently in an active state (not delivered/cancelled) */
    private long pendingOrders;

    // ── Revenue ────────────────────────────────────────────────────────
    /** Total platform revenue from all paid orders */
    private BigDecimal totalRevenue;

    /** Revenue this calendar month */
    private BigDecimal monthlyRevenue;

    /** Revenue today */
    private BigDecimal todayRevenue;

    // ── Chart data ─────────────────────────────────────────────────────
    /** Last 7 days revenue: { "2024-01-15": 12500.00, ... } */
    private Map<String, BigDecimal> dailyRevenue;

    /** Orders grouped by status: { "PENDING": 12, "DELIVERED": 45, ... } */
    private Map<String, Long> ordersByStatus;
}
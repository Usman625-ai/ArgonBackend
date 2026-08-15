package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Real, live platform statistics shown on the public landing and About
 * pages (verified sellers, customers, products, satisfaction rate).
 * Unlike DashboardStatsResponse, this is safe to expose with no auth —
 * it contains only aggregate counts, no individual user/order data.
 */
@Data
@Builder
public class PlatformStatsResponse {

    /** Sellers with SellerStatus.APPROVED — i.e. actually live on the storefront. */
    private long verifiedSellers;

    /** Registered customers (Role.CUSTOMER). */
    private long totalCustomers;

    /** Active, publicly listed products. */
    private long totalProducts;

    /**
     * Average product rating across all reviews on the platform, 0–5.
     * Null when there are no reviews yet — the frontend should hide the
     * satisfaction stat in that case rather than show a fabricated number.
     */
    private Double averageRating;
}
package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.UpdateSellerProfileRequest;
import com.ecommerce.multivendor.dto.response.ApiResponse;
import com.ecommerce.multivendor.dto.response.MonthlyRevenueResponse;
import com.ecommerce.multivendor.dto.response.SellerDashboardResponse;
import com.ecommerce.multivendor.dto.response.UserResponse;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.enums.SellerStatus;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.repository.OrderRepository;
import com.ecommerce.multivendor.repository.ProductRepository;
import com.ecommerce.multivendor.repository.UserRepository;
import com.ecommerce.multivendor.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SellerService {

    private final SecurityUtils securityUtils;
    private final UserRepository     userRepository;
    private final OrderRepository    orderRepository;
    private final ProductRepository  productRepository;
    private final AdminService       adminService;

    // ─── Seller Dashboard Stats ────────────────────────────────────────

    @Transactional(readOnly = true)
    public SellerDashboardResponse getSellerStats(Long sellerId) {

        // ── Product counts ──────────────────────────────────────────────
        long totalProducts      = productRepository.countActiveProductsBySeller(sellerId);
        long lowStockProducts   = productRepository.countLowStockProductsBySeller(sellerId, 10);
        long outOfStockProducts = productRepository.countOutOfStockProductsBySeller(sellerId);

        // ── Order counts ────────────────────────────────────────────────
        long totalOrders     = orderRepository.countActiveOrdersBySeller(sellerId);
        long pendingOrders   = orderRepository.countPendingOrdersBySeller(sellerId);
        long activeOrders    = orderRepository.countActiveInProgressOrdersBySeller(sellerId);
        long deliveredOrders = orderRepository.countDeliveredOrdersBySeller(sellerId);
        long cancelledOrders = orderRepository.countCancelledOrdersBySeller(sellerId);

        // ── Revenue ─────────────────────────────────────────────────────
        BigDecimal totalRevenue = orderRepository.calculateRevenueForSeller(sellerId);

        LocalDateTime monthStart = LocalDateTime.now()
                .withDayOfMonth(1).toLocalDate().atStartOfDay();
        BigDecimal monthlyRevenue = orderRepository
                .calculateRevenueForSellerByDateRange(sellerId, monthStart, LocalDateTime.now());

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        BigDecimal todayRevenue = orderRepository
                .calculateRevenueForSellerByDateRange(sellerId, todayStart, LocalDateTime.now());

        // ── Orders-by-status map ─────────────────────────────────────────
        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        orderRepository.countOrdersByStatusForSeller(sellerId)
                .forEach(row -> ordersByStatus.put(
                        row[0].toString(),
                        ((Number) row[1]).longValue()
                ));

        // ── Daily revenue (last 7 days) ──────────────────────────────────
        Map<String, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        orderRepository.getDailyRevenueForSeller(
                        sellerId, LocalDateTime.now().minusDays(7))
                .forEach(row -> dailyRevenue.put(
                        row[0].toString(),
                        row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO
                ));

        // ── Shop info ────────────────────────────────────────────────────
        User seller = userRepository.findById(sellerId).orElseThrow();

        return SellerDashboardResponse.builder()
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .outOfStockProducts(outOfStockProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .activeOrders(activeOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue    != null ? totalRevenue    : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue     != null ? todayRevenue   : BigDecimal.ZERO)
                .shopName(seller.getShopName())
                .sellerStatus(seller.getSellerStatus() != null
                        ? seller.getSellerStatus().name() : "UNKNOWN")
                .ordersByStatus(ordersByStatus)
                .dailyRevenue(dailyRevenue)
                .build();
    }

    // ─── Seller Profile ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getSellerProfile(User seller) {
        return adminService.toUserResponse(seller);
    }

    public UserResponse updateSellerProfile(User seller, UpdateSellerProfileRequest request) {
        if (seller.getSellerStatus() != SellerStatus.APPROVED) {
            throw new BadRequestException(
                    "Only approved sellers can update their profile. Current status: "
                            + seller.getSellerStatus());
        }

        if (request.getShopName()          != null) seller.setShopName(request.getShopName());
        if (request.getShopDescription()   != null) seller.setShopDescription(request.getShopDescription());
        if (request.getShopLogo()          != null) seller.setShopLogo(request.getShopLogo());
        if (request.getShopBanner()        != null) seller.setShopBanner(request.getShopBanner());
        if (request.getGstNumber()         != null) seller.setGstNumber(request.getGstNumber());
        if (request.getPanNumber()         != null) seller.setPanNumber(request.getPanNumber());
        if (request.getContactNumber()     != null) seller.setContactNumber(request.getContactNumber());
        if (request.getBankAccountNumber() != null) seller.setBankAccountNumber(request.getBankAccountNumber());
        if (request.getBankIfsc()          != null) seller.setBankIfsc(request.getBankIfsc());
        if (request.getBankName()          != null) seller.setBankName(request.getBankName());

        userRepository.save(seller);
        log.info("Seller profile updated: {}", seller.getId());
        return adminService.toUserResponse(seller);
    }

    // ─── Guard: seller must be approved ───────────────────────────────

    public void requireApprovedSeller(User seller) {
        if (seller.getSellerStatus() != SellerStatus.APPROVED) {
            throw new BadRequestException(
                    "Your seller account status is " + seller.getSellerStatus()
                            + ". Admin approval is required before you can perform this action.");
        }
    }

//    Revenue

    @Transactional(readOnly = true)
    public ApiResponse<MonthlyRevenueResponse> getMonthlyRevenue(Integer year) {
        User currentSeller = securityUtils.getCurrentUser();
        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        List<Object[]> results = orderRepository.findMonthlyRevenueBySellerAndYear(
                currentSeller.getId(), targetYear);

        List<MonthlyRevenueResponse.MonthlyData> monthlyDataList = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0;

        for (int month = 1; month <= 12; month++) {
            Month m = Month.of(month);
            monthlyDataList.add(MonthlyRevenueResponse.MonthlyData.builder()
                    .month(month)
                    .monthName(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .revenue(BigDecimal.ZERO)
                    .orderCount(0L)
                    .productCount(0L)
                    .build());
        }

        for (Object[] row : results) {
            int month = ((Number) row[0]).intValue();
            BigDecimal revenue = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            long orderCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long productCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;

            int index = month - 1;
            monthlyDataList.set(index, MonthlyRevenueResponse.MonthlyData.builder()
                    .month(month)
                    .monthName(Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .revenue(revenue.setScale(2, RoundingMode.HALF_UP))
                    .orderCount(orderCount)
                    .productCount(productCount)
                    .build());

            totalRevenue = totalRevenue.add(revenue);
            totalOrders += orderCount;
        }

        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        MonthlyRevenueResponse response = MonthlyRevenueResponse.builder()
                .year(targetYear)
                .monthlyData(monthlyDataList)
                .totalRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP))
                .totalOrders(totalOrders)
                .averageOrderValue(avgOrderValue)
                .build();

        return ApiResponse.success("Monthly revenue data retrieved successfully", response);
    }

    private User getCurrentSeller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
    }
}
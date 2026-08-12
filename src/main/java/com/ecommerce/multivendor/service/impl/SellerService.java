package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.UpdateSellerProfileRequest;
import com.ecommerce.multivendor.dto.response.ApiResponse;
import com.ecommerce.multivendor.dto.response.MonthlyRevenueResponse;
import com.ecommerce.multivendor.dto.response.SellerDashboardResponse;
import com.ecommerce.multivendor.dto.response.SellerPublicProfileResponse;
import com.ecommerce.multivendor.dto.response.UserResponse;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.enums.Role;
import com.ecommerce.multivendor.enums.SellerStatus;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.OrderRepository;
import com.ecommerce.multivendor.repository.ProductRepository;
import com.ecommerce.multivendor.repository.ReviewRepository;
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


    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final UserRepository     userRepository;
    private final OrderRepository    orderRepository;
    private final ProductRepository  productRepository;
    private final ReviewRepository   reviewRepository;
    private final AdminService       adminService;
    private final CloudinaryService  cloudinaryService;

    // ─── Seller Dashboard Stats ────────────────────────────────────────

    @Transactional(readOnly = true)
    public SellerPublicProfileResponse getSellerPublicProfile(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));

        if (seller.getRole() != Role.SELLER || seller.getSellerStatus() != SellerStatus.APPROVED || !seller.isActive()) {
            throw new ResourceNotFoundException("Seller", sellerId);
        }

        long totalReviews = reviewRepository.countReviewsForSeller(sellerId);
        Double avgRating = totalReviews > 0 ? reviewRepository.calculateAverageRatingForSeller(sellerId) : null;

        return SellerPublicProfileResponse.builder()
                .id(seller.getId())
                .name(seller.getName())
                .shopName(seller.getShopName())
                .shopDescription(seller.getShopDescription())
                .shopLogo(seller.getShopLogo())
                .shopBanner(seller.getShopBanner())
                .memberSince(seller.getCreatedAt())
                .totalProducts(productRepository.countActiveProductsBySeller(sellerId))
                .ordersDelivered(orderRepository.countDeliveredOrdersBySeller(sellerId))
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : null)
                .totalReviews(totalReviews)
                .build();
    }

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
        if (seller.getSellerStatus() != SellerStatus.APPROVED
                && seller.getSellerStatus() != SellerStatus.REJECTED) {
            throw new BadRequestException(
                    "Only approved or rejected sellers can update their profile. Current status: "
                            + seller.getSellerStatus());
        }

        if (request.getName()              != null) seller.setName(request.getName());
        if (request.getProfileImage()      != null) {
            if (!request.getProfileImage().equals(seller.getProfileImage())) {
                cloudinaryService.deleteImageByUrl(seller.getProfileImage());
            }
            seller.setProfileImage(request.getProfileImage());
        }
        if (request.getShopName()          != null) seller.setShopName(request.getShopName());
        if (request.getShopDescription()   != null) seller.setShopDescription(request.getShopDescription());
        if (request.getShopLogo()          != null) {
            if (!request.getShopLogo().equals(seller.getShopLogo())) {
                cloudinaryService.deleteImageByUrl(seller.getShopLogo());
            }
            seller.setShopLogo(request.getShopLogo());
        }
        if (request.getShopBanner()        != null) {
            if (!request.getShopBanner().equals(seller.getShopBanner())) {
                cloudinaryService.deleteImageByUrl(seller.getShopBanner());
            }
            seller.setShopBanner(request.getShopBanner());
        }
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

    // ─── Reapply after rejection ───────────────────────────────────────

    public UserResponse reapplyAsSeller(User seller, UpdateSellerProfileRequest request) {
        if (seller.getSellerStatus() != SellerStatus.REJECTED) {
            throw new BadRequestException(
                    "Only rejected sellers can reapply. Current status: " + seller.getSellerStatus());
        }

        if (request.getShopName() == null || request.getShopName().trim().isEmpty()) {
            throw new BadRequestException("Shop name is required to reapply");
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


        seller.setSellerStatus(SellerStatus.PENDING);
        seller.setRejectionReason(null);
        userRepository.save(seller);

        notificationService.createNotification(seller,
                "Your reapplication has been submitted and is pending admin review.",
                "Reapplication Submitted",
                com.ecommerce.multivendor.enums.NotificationType.GENERAL,
                "/seller/dashboard");

// Notify all admins so the reapplication gets reviewed, mirroring the
// notification sent for a brand-new seller registration.
        final String shopName = seller.getShopName();
        final String sellerEmail = seller.getEmail();
        userRepository.findByRole(com.ecommerce.multivendor.enums.Role.ADMIN).forEach(admin ->
                notificationService.createNotification(admin,
                        shopName + " (" + sellerEmail + ") was previously rejected and has reapplied — please review.",
                        "Seller Reapplication Pending Review",
                        com.ecommerce.multivendor.enums.NotificationType.GENERAL,
                        "/admin/sellers"));
        log.info("Seller {} reapplied after rejection", seller.getId());
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
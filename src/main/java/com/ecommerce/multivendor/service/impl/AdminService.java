package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.ProductStatusUpdateRequest;
import com.ecommerce.multivendor.dto.request.SystemSettingsRequest;
import com.ecommerce.multivendor.dto.response.*;
import com.ecommerce.multivendor.entity.GlobalSettings;
import com.ecommerce.multivendor.entity.Product;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.enums.NotificationType;
import com.ecommerce.multivendor.enums.Role;
import com.ecommerce.multivendor.enums.SellerStatus;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.GlobalSettingsRepository;
import com.ecommerce.multivendor.repository.OrderRepository;
import com.ecommerce.multivendor.repository.ProductRepository;
import com.ecommerce.multivendor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final GlobalSettingsRepository settingsRepo;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // ─── Dashboard Statistics ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {

        // ── User counts ────────────────────────────────────────────────
        long totalCustomers     = userRepository.countByRole(Role.CUSTOMER);
        long totalSellers       = userRepository.countByRole(Role.SELLER);
        long approvedSellers    = userRepository.countByRoleAndSellerStatus(
                Role.SELLER, SellerStatus.APPROVED);
        long pendingApprovals   = userRepository.countByRoleAndSellerStatus(
                Role.SELLER, SellerStatus.PENDING);

        // ── Product & order counts ─────────────────────────────────────
        long totalProducts  = productRepository.countByActiveTrue();
        long totalOrders    = orderRepository.count();
        long pendingOrders  = orderRepository.countActiveOrders();

        // ── Revenue ────────────────────────────────────────────────────
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();

        BigDecimal monthlyRevenue = orderRepository.calculateRevenueByDateRange(
                LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay(),
                LocalDateTime.now());

        BigDecimal todayRevenue = orderRepository.calculateRevenueByDateRange(
                LocalDateTime.now().toLocalDate().atStartOfDay(),
                LocalDateTime.now());

        // ── Daily revenue chart (last 7 days) ──────────────────────────
        Map<String, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        orderRepository.getDailyRevenue(LocalDateTime.now().minusDays(7))
                .forEach(row -> dailyRevenue.put(row[0].toString(), (BigDecimal) row[1]));

        return DashboardStatsResponse.builder()
                .totalUsers(totalCustomers + totalSellers)
                .totalSellers(totalSellers)
                .approvedSellers(approvedSellers)
                .pendingSellerApprovals(pendingApprovals)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .totalRevenue(totalRevenue   != null ? totalRevenue   : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue   != null ? todayRevenue   : BigDecimal.ZERO)
                .dailyRevenue(dailyRevenue)
                .build();
    }



    // ─── Seller Management ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getSellers(SellerStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> sellersPage = status != null
                ? userRepository.findByRoleAndSellerStatus(Role.SELLER, status, pageable)
                : userRepository.findByRole(Role.SELLER, pageable);

        return toPagedResponse(sellersPage);
    }

    @Transactional(readOnly = true)
    public UserResponse getSellerById(Long id) {
        User seller = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", id));
        if (seller.getRole() != Role.SELLER) {
            throw new BadRequestException("User is not a seller");
        }
        return toUserResponse(seller);
    }

    public UserResponse approveSeller(Long sellerId) {
        User seller = getSellerEntity(sellerId);
        if (seller.getSellerStatus() == SellerStatus.APPROVED) {
            throw new BadRequestException("Seller is already approved");
        }

        seller.setSellerStatus(SellerStatus.APPROVED);
        seller.setActive(true);
        userRepository.save(seller);

        emailService.sendSellerApprovalEmail(seller);
        notificationService.createNotification(seller,
                "Your seller account has been approved! Start selling now.",
                "Account Approved", NotificationType.SELLER_APPROVED, "/seller/dashboard");

        log.info("Seller {} approved", sellerId);
        return toUserResponse(seller);
    }

    public UserResponse rejectSeller(Long sellerId, String reason) {
        User seller = getSellerEntity(sellerId);

        seller.setSellerStatus(SellerStatus.REJECTED);
        seller.setRejectionReason(reason);
        userRepository.save(seller);

        emailService.sendSellerRejectionEmail(seller, reason);
        notificationService.createNotification(seller,
                "Your seller registration was not approved. Reason: " + reason,
                "Registration Rejected", NotificationType.SELLER_REJECTED, "/seller/register");

        log.info("Seller {} rejected. Reason: {}", sellerId, reason);
        return toUserResponse(seller);
    }

    public UserResponse toggleSellerStatus(Long sellerId, boolean enable) {
        User seller = getSellerEntity(sellerId);
        seller.setActive(enable);
        if (!enable) {
            seller.setSellerStatus(SellerStatus.SUSPENDED);
        } else if (seller.getSellerStatus() == SellerStatus.SUSPENDED) {
            seller.setSellerStatus(SellerStatus.APPROVED);
        }
        userRepository.save(seller);
        log.info("Seller {} {}", sellerId, enable ? "enabled" : "disabled");
        return toUserResponse(seller);
    }

    // ─── Customer Management ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getCustomers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> customersPage = userRepository.findByRole(Role.CUSTOMER, pageable);
        return toPagedResponse(customersPage);
    }

    public UserResponse toggleUserStatus(Long userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setActive(enable);
        userRepository.save(user);
        return toUserResponse(user);
    }

    // ─── Helpers & Mappers ─────────────────────────────────────────────────

    private User getSellerEntity(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));
        if (seller.getRole() != Role.SELLER) {
            throw new BadRequestException("User is not a seller");
        }
        return seller;
    }

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .verified(user.isVerified())
                .contactNumber(user.getContactNumber())
                .profileImage(user.getProfileImage())
                .sellerStatus(user.getSellerStatus())
                .shopName(user.getShopName())
                .shopDescription(user.getShopDescription())
                .shopLogo(user.getShopLogo())
                .shopBanner(user.getShopBanner())
                .gstNumber(user.getGstNumber())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private PagedResponse<UserResponse> toPagedResponse(Page<User> page) {
        return PagedResponse.<UserResponse>builder()
                .content(page.getContent().stream().map(this::toUserResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    @Transactional
    public ApiResponse<Product> toggleProductStatus(Long productId, ProductStatusUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        product.setActive(request.getActive());
        Product updated = productRepository.save(product);

        String message = Boolean.TRUE.equals(request.getActive())
                ? "Product activated successfully"
                : "Product deactivated successfully";

        return ApiResponse.success(message, updated);
    }

//    public SystemSettingsResponse getSettings() {
//        GlobalSettings s = settingsRepo.findById(1L)
//                .orElse(GlobalSettings.builder().id(1L).build());
//        return SystemSettingsResponse.builder()
//                .maintenanceMode()
//                .allowSellerRegistration(s.isAllowSellerRegistration())
//                .build();
//    }
//
//    public SystemSettingsResponse updateSettings(SystemSettingsRequest req) {
//        SystemSettings s = settingsRepo.findById(1L)
//                .orElse(SystemSettings.builder().id(1L).build());
//        if (req.getMaintenanceMode() != null) s.setMaintenanceMode(req.getMaintenanceMode());
//        if (req.getAllowSellerRegistration() != null) s.setAllowSellerRegistration(req.getAllowSellerRegistration());
//        settingsRepo.save(s);
//        return getSettings();
//    }
}

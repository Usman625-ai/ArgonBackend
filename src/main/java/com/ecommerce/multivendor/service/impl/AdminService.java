package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.ProductStatusUpdateRequest;
import com.ecommerce.multivendor.dto.response.*;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final com.ecommerce.multivendor.repository.ReviewRepository reviewRepository;
    private final ProductService productService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    // ─── Dashboard Statistics ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {

        // ── User counts ────────────────────────────────────────────────
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long totalSellers = userRepository.countByRole(Role.SELLER);
        long approvedSellers = userRepository.countByRoleAndSellerStatus(
                Role.SELLER, SellerStatus.APPROVED);
        long pendingApprovals = userRepository.countByRoleAndSellerStatus(
                Role.SELLER, SellerStatus.PENDING);

        // ── Product & order counts ─────────────────────────────────────
        long totalProducts = productRepository.countByActiveTrue();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countPendingOrders();

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
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
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
        return getCustomers(page, size, null);
    }

    public PagedResponse<UserResponse> getCustomers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> customersPage = (search != null && !search.trim().isEmpty())
                ? userRepository.searchByRoleAndKeyword(Role.CUSTOMER, search.trim(), pageable)
                : userRepository.findByRole(Role.CUSTOMER, pageable);
        return toPagedResponse(customersPage);
    }

    /**
     * Permanently deletes a single customer account and all their data (orders,
     * addresses, cart, wishlist, notifications via cascade; reviews explicitly
     * first, since Review has no cascade from User). Irreversible.
     */
    @Transactional
    public void deleteCustomer(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        if (customer.getRole() != Role.CUSTOMER) {
            throw new BadRequestException("Only customer accounts can be deleted with this action");
        }
        reviewRepository.deleteByUserId(customer.getId());
        userRepository.delete(customer);
        log.info("Admin deleted customer account: {} ({})", customer.getEmail(), customerId);
    }

    public UserResponse toggleUserStatus(Long userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setActive(enable);
        userRepository.save(user);
        return toUserResponse(user);
    }

    /**
     * Permanently deletes every CUSTOMER account, along with their orders, addresses,
     * cart items, wishlist entries, and notifications (all cascade from the User entity),
     * plus their reviews (deleted explicitly first, since Review has no cascade from User
     * and would otherwise hit a foreign-key constraint). Sellers and admins are untouched.
     *
     * This is irreversible — the caller (admin UI) is expected to confirm with the user
     * before calling this.
     */
    @Transactional
    public int deleteAllCustomers() {
        List<User> customers = userRepository.findByRole(Role.CUSTOMER, Pageable.unpaged()).getContent();
        int count = customers.size();
        for (User customer : customers) {
            reviewRepository.deleteByUserId(customer.getId());
            userRepository.delete(customer);
        }
        log.info("Admin bulk-delete: removed {} customer accounts and all their data", count);
        return count;
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

    // ─── Admin's own profile (personal info) ───────────────────────────────

    public UserResponse getAdminProfile(User admin) {
        return toUserResponse(admin);
    }

    public UserResponse updateAdminProfile(User admin, com.ecommerce.multivendor.dto.request.UpdateAdminProfileRequest request) {
        if (request.getName()          != null) admin.setName(request.getName());
        if (request.getContactNumber() != null) admin.setContactNumber(request.getContactNumber());
        if (request.getProfileImage()  != null) {
            if (!request.getProfileImage().equals(admin.getProfileImage())) {
                cloudinaryService.deleteImageByUrl(admin.getProfileImage());
            }
            admin.setProfileImage(request.getProfileImage());
        }
        userRepository.save(admin);
        log.info("Admin profile updated: {}", admin.getId());
        return toUserResponse(admin);
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
    public ApiResponse<ProductResponse> toggleProductStatus(Long productId, ProductStatusUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        boolean active = Boolean.TRUE.equals(request.getActive());
        product.setActive(active);
        // Deactivating via admin locks it so only admin can bring it back;
        // (re)activating via admin always clears the lock.
        product.setAdminLocked(!active);
        Product updated = productRepository.save(product);

        String message = active
                ? "Product activated successfully"
                : "Product deactivated successfully";

        return ApiResponse.success(message, productService.toProductResponse(updated));
    }
}
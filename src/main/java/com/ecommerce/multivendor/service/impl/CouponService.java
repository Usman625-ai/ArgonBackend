package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.CouponRequest;
import com.ecommerce.multivendor.dto.request.CouponValidateRequest;
import com.ecommerce.multivendor.dto.response.CouponResponse;
import com.ecommerce.multivendor.dto.response.PagedResponse;
import com.ecommerce.multivendor.entity.Coupon;
import com.ecommerce.multivendor.enums.DiscountType;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.CouponRepository;
import com.ecommerce.multivendor.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    // ─── Admin CRUD ────────────────────────────────────────────────────────

    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new BadRequestException("Coupon code already exists: " + request.getCode());
        }
        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new BadRequestException("Valid from date must be before valid until date");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE &&
                request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Percentage discount cannot exceed 100");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase().trim())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue() != null
                        ? request.getMinOrderValue() : BigDecimal.ZERO)
                .maxDiscount(request.getMaxDiscount())
                .validFrom(request.getValidFrom().atStartOfDay())
                .validUntil(request.getValidUntil().atTime(23, 59, 59))
                .usageLimit(request.getUsageLimit())
                .perUserLimit(request.getPerUserLimit())
                .active(true)
                .build();

        coupon = couponRepository.save(coupon);
        log.info("Coupon created: {}", coupon.getCode());
        return toCouponResponse(coupon, null);
    }


    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id));

        String newCode = request.getCode().toUpperCase().trim();
        if (!newCode.equals(coupon.getCode()) && couponRepository.existsByCode(newCode)) {
            throw new BadRequestException("Coupon code already exists: " + newCode);
        }
        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new BadRequestException("Valid from date must be before valid until date");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE &&
                request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Percentage discount cannot exceed 100");
        }

        coupon.setCode(newCode);
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setValidFrom(request.getValidFrom().atStartOfDay());
        coupon.setValidUntil(request.getValidUntil().atTime(23, 59, 59));
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerUserLimit(request.getPerUserLimit());

        coupon = couponRepository.save(coupon);
        log.info("Coupon updated: {}", coupon.getCode());
        return toCouponResponse(coupon, null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CouponResponse> getAllCoupons(int page, int size) {
        Page<Coupon> couponPage = couponRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return PagedResponse.<CouponResponse>builder()
                .content(couponPage.getContent().stream()
                        .map(c -> toCouponResponse(c, null)).toList())
                .pageNumber(couponPage.getNumber())
                .pageSize(couponPage.getSize())
                .totalElements(couponPage.getTotalElements())
                .totalPages(couponPage.getTotalPages())
                .last(couponPage.isLast())
                .first(couponPage.isFirst())
                .build();
    }

    /**
     * Permanently deletes the coupon. Safe to hard-delete: orders store the
     * coupon code they used as a plain string snapshot (Order.couponCode),
     * not a foreign key to this row, so past order history is unaffected.
     */
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id));
        couponRepository.delete(coupon);
        log.info("Coupon deleted: {}", coupon.getCode());
    }

    // ─── Customer: Active coupons list (for cart/checkout dropdown) ────────

    @Transactional(readOnly = true)
    public List<CouponResponse> getActiveCoupons(BigDecimal orderAmount) {
        List<Coupon> coupons = couponRepository.findAllCurrentlyValid();
        return coupons.stream()
                .map(c -> toCouponResponse(c,
                        orderAmount != null ? c.calculateDiscount(orderAmount) : null))
                .toList();
    }

    // ─── Customer: Validate & Apply ────────────────────────────────────────

    @Transactional(readOnly = true)
    public CouponResponse validateCoupon(CouponValidateRequest request, Long customerId) {
        Coupon coupon = couponRepository.findByCode(request.getCouponCode().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

        if (!coupon.isValid()) {
            throw new BadRequestException("Coupon is expired or no longer valid");
        }
        if (request.getOrderAmount().compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BadRequestException("Minimum order value of pkr"
                    + coupon.getMinOrderValue() + " required for this coupon");
        }
        if (customerId != null && coupon.getPerUserLimit() > 0) {
            long usedByUser = orderRepository.countByCustomerIdAndCouponCode(customerId, coupon.getCode());
            if (usedByUser >= coupon.getPerUserLimit()) {
                throw new BadRequestException("You've already used this coupon the maximum number of times");
            }
        }

        BigDecimal discount = coupon.calculateDiscount(request.getOrderAmount());
        return toCouponResponse(coupon, discount);
    }

    /**
     * Apply coupon to an order - increment usage count.
     * Must be called within an order creation transaction.
     */
    public BigDecimal applyCoupon(String couponCode, BigDecimal orderAmount, Long customerId) {
        if (couponCode == null || couponCode.isBlank()) return BigDecimal.ZERO;

        Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code: " + couponCode));

        if (!coupon.isValid()) {
            throw new BadRequestException("Coupon is no longer valid");
        }
        if (customerId != null && coupon.getPerUserLimit() > 0) {
            long usedByUser = orderRepository.countByCustomerIdAndCouponCode(customerId, coupon.getCode());
            if (usedByUser >= coupon.getPerUserLimit()) {
                throw new BadRequestException("You've already used this coupon the maximum number of times");
            }
        }

        BigDecimal discount = coupon.calculateDiscount(orderAmount);
        couponRepository.incrementUsedCount(coupon.getId());
        log.info("Coupon {} applied. Discount: {}", couponCode, discount);
        return discount;
    }

    // ─── Mapper ────────────────────────────────────────────────────────────

    private CouponResponse toCouponResponse(Coupon coupon, BigDecimal applicableDiscount) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscount(coupon.getMaxDiscount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .active(coupon.isActive())
                .valid(coupon.isValid())
                .applicableDiscount(applicableDiscount)
                .build();
    }
}
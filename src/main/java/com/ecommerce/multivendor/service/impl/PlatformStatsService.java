package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.response.PlatformStatsResponse;
import com.ecommerce.multivendor.enums.Role;
import com.ecommerce.multivendor.enums.SellerStatus;
import com.ecommerce.multivendor.repository.ProductRepository;
import com.ecommerce.multivendor.repository.ReviewRepository;
import com.ecommerce.multivendor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the public "verified sellers / customers / products / satisfaction"
 * stat counters on the landing and About pages. Every number here is a
 * live aggregate query — nothing hardcoded.
 */
@Service
@RequiredArgsConstructor
public class PlatformStatsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        long verifiedSellers = userRepository.countByRoleAndSellerStatus(Role.SELLER, SellerStatus.APPROVED);
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long totalProducts = productRepository.countByActiveTrue();
        Double averageRating = reviewRepository.calculatePlatformAverageRating();

        return PlatformStatsResponse.builder()
                .verifiedSellers(verifiedSellers)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .averageRating(averageRating) // null when there are no reviews yet
                .build();
    }
}
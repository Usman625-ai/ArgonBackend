package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.ReviewRequest;
import com.ecommerce.multivendor.dto.response.CustomerPublicProfileResponse;
import com.ecommerce.multivendor.dto.response.PagedResponse;
import com.ecommerce.multivendor.dto.response.ReviewResponse;
import com.ecommerce.multivendor.entity.Product;
import com.ecommerce.multivendor.entity.Review;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.OrderItemRepository;
import com.ecommerce.multivendor.repository.ProductRepository;
import com.ecommerce.multivendor.repository.ReviewRepository;
import com.ecommerce.multivendor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public ReviewResponse addReview(ReviewRequest request, User user) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        if (reviewRepository.existsByProductIdAndUserId(product.getId(), user.getId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        // Check verified purchase
        boolean verifiedPurchase = !orderItemRepository
                .findDeliveredByCustomerAndProduct(user.getId(), product.getId()).isEmpty();

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .verifiedPurchase(verifiedPurchase)
                .build();

        review = reviewRepository.save(review);

        // Recalculate product rating
        updateProductRating(product.getId());

        return toReviewResponse(review, user.getName());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByProductId(
                productId, PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return PagedResponse.<ReviewResponse>builder()
                .content(reviewPage.getContent().stream()
                        .map(r -> toReviewResponse(r, r.getUser().getName())).toList())
                .pageNumber(reviewPage.getNumber())
                .pageSize(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .first(reviewPage.isFirst())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getCustomerReviews(Long userId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)
        );
        return PagedResponse.<ReviewResponse>builder()
                .content(reviewPage.getContent().stream().map(this::toReviewResponseWithProduct).toList())
                .pageNumber(reviewPage.getNumber())
                .pageSize(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .first(reviewPage.isFirst())
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerPublicProfileResponse getCustomerPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.isActive()) {
            throw new ResourceNotFoundException("User", userId);
        }

        long totalReviews = reviewRepository.countByUserId(userId);
        if (totalReviews == 0) {
            // No public footprint (no reviews) — nothing to show, treat like not found
            // rather than exposing an otherwise-empty profile for any random user id.
            throw new ResourceNotFoundException("User", userId);
        }

        return CustomerPublicProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .memberSince(user.getCreatedAt())
                .totalReviews(totalReviews)
                .build();
    }

    private void updateProductRating(Long productId) {
        Double avg = reviewRepository.calculateAverageRating(productId);
        long count = reviewRepository.countByProductId(productId);
        BigDecimal rating = avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        productRepository.updateRatingStats(productId, rating, (int) count);
    }

    private ReviewResponse toReviewResponse(Review review, String userName) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(review.getUser().getId())
                .userName(userName)
                .userProfileImage(review.getUser().getProfileImage())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .helpfulCount(review.getHelpfulCount())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private ReviewResponse toReviewResponseWithProduct(Review review) {
        Product product = review.getProduct();
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productImage(product.getImages().stream()
                        .filter(img -> img.isPrimary()).findFirst()
                        .or(() -> product.getImages().stream().findFirst())
                        .map(img -> img.getImageUrl()).orElse(null))
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .userProfileImage(review.getUser().getProfileImage())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .helpfulCount(review.getHelpfulCount())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
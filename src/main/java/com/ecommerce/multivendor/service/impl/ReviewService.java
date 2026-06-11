package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.ReviewRequest;
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
            .rating(review.getRating())
            .comment(review.getComment())
            .verifiedPurchase(review.isVerifiedPurchase())
            .helpfulCount(review.getHelpfulCount())
            .createdAt(review.getCreatedAt())
            .build();
    }
}

package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductId(Long productId, Pageable pageable);
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double calculateAverageRating(@Param("productId") Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.seller.id = :sellerId")
    Double calculateAverageRatingForSeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.seller.id = :sellerId")
    long countReviewsForSeller(@Param("sellerId") Long sellerId);

    long countByProductId(Long productId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUserId(Long userId);
}
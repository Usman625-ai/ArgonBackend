package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    boolean existsByCode(String code);
    Page<Coupon> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying @Transactional
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :id")
    void incrementUsedCount(@Param("id") Long id);

    @Query("SELECT c FROM Coupon c WHERE c.active = true " +
            "AND c.validFrom <= CURRENT_TIMESTAMP AND c.validUntil >= CURRENT_TIMESTAMP " +
            "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit) " +
            "ORDER BY c.discountValue DESC")
    List<Coupon> findAllCurrentlyValid();
}
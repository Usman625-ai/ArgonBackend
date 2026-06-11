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

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    boolean existsByCode(String code);
    Page<Coupon> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying @Transactional
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :id")
    void incrementUsedCount(@Param("id") Long id);
}

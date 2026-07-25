package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.ChangePasswordOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChangePasswordOtpRepository extends JpaRepository<ChangePasswordOtp, Long> {
    Optional<ChangePasswordOtp> findByUserId(Long userId);

    @Modifying @Transactional
    void deleteByUserId(Long userId);

    @Modifying @Transactional
    @Query("DELETE FROM ChangePasswordOtp c WHERE c.otpExpiry < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}


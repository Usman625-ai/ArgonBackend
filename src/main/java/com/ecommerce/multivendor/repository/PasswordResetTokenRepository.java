package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);

    @Modifying @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now OR t.used = true")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}

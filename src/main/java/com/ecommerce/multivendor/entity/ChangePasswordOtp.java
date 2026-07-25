package com.ecommerce.multivendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "change_password_otps", indexes = {
        @Index(name = "idx_cp_otp_user", columnList = "user_id", unique = true)
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChangePasswordOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "otp_expiry", nullable = false)
    private LocalDateTime otpExpiry;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(otpExpiry);
    }
}

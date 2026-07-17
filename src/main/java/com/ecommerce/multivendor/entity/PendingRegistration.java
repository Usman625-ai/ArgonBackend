package com.ecommerce.multivendor.entity;

import com.ecommerce.multivendor.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pending_registrations", indexes = {
        @Index(name = "idx_pending_email", columnList = "email", unique = true)
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "verification_otp", nullable = false)
    private String verificationOtp;

    @Column(name = "otp_expiry", nullable = false)
    private LocalDateTime otpExpiry;

    @Column(name = "last_otp_sent_at", nullable = false)
    private LocalDateTime lastOtpSentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
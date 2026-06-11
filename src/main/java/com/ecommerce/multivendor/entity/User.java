package com.ecommerce.multivendor.entity;

import com.ecommerce.multivendor.enums.Role;
import com.ecommerce.multivendor.enums.SellerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_role", columnList = "role")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

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

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_verified")
    @Builder.Default
    private boolean verified = false;

    @Column(name = "verification_otp")
    private String verificationOtp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "profile_image")
    private String profileImage;

    // Seller-specific fields

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_status")
    private SellerStatus sellerStatus;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "shop_description", columnDefinition = "TEXT")
    private String shopDescription;

    @Column(name = "shop_logo")
    private String shopLogo;

    @Column(name = "shop_banner")
    private String shopBanner;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_ifsc")
    private String bankIfsc;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Relationships

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Wishlist> wishlistItems = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

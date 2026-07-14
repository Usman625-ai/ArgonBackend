package com.ecommerce.multivendor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", nullable = false)
    @Builder.Default
    private String siteName = "ShopVersee";

    @Column(name = "contact_email", nullable = false)
    @Builder.Default
    private String contactEmail = "support@shopversee.com";

    @Column(name = "currency_symbol", nullable = false)
    @Builder.Default
    private String currencySymbol = "PKR";

    @Column(name = "currency_code", nullable = false)
    @Builder.Default
    private String currencyCode = "PKR";

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "tax_rate")
    @Builder.Default
    private Double taxRate = 0.0;

    @Column(name = "shipping_fee")
    @Builder.Default
    private Double shippingFee = 0.0;

    @Column(name = "free_shipping_threshold")
    private Double freeShippingThreshold;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
package com.ecommerce.multivendor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_address_user", columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+92|0)[0-9]{9,10}$", message = "Invalid Pakistan phone number")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @NotBlank(message = "Address line 1 is required")
    @Size(min = 5, max = 255)
    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String city;

    @NotBlank(message = "State/Province is required")
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[0-9]{5}$", message = "Postal code must be 5 digits")
    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false)
    @Builder.Default
    private String country = "India";

    @Column(name = "is_default")
    @Builder.Default
    private boolean defaultAddress = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.isEmpty()) sb.append(", ").append(addressLine2);
        sb.append(", ").append(city).append(", ").append(state);
        sb.append(" - ").append(pincode).append(", ").append(country);
        return sb.toString();
    }
}

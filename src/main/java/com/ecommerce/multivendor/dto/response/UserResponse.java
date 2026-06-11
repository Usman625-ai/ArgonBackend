package com.ecommerce.multivendor.dto.response;

import com.ecommerce.multivendor.enums.Role;
import com.ecommerce.multivendor.enums.SellerStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean active;
    private boolean verified;
    private String contactNumber;
    private String profileImage;
    // Seller-specific
    private SellerStatus sellerStatus;
    private String shopName;
    private String shopDescription;
    private String shopLogo;
    private String shopBanner;
    private String gstNumber;
    private LocalDateTime createdAt;
}

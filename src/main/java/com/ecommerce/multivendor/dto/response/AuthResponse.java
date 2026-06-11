package com.ecommerce.multivendor.dto.response;

import com.ecommerce.multivendor.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuthResponse {
    private Long userId;
    private String name;
    private String email;
    private Role role;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private boolean verified;
    private boolean sellerApproved;
}

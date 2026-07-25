package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Body for POST /api/auth/change-password/request-otp */
@Data
public class ChangePasswordOtpRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;
}


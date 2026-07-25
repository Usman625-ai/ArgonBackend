package com.ecommerce.multivendor.controller;

import com.ecommerce.multivendor.dto.request.*;
import com.ecommerce.multivendor.dto.response.ApiResponse;
import com.ecommerce.multivendor.dto.response.AuthResponse;
import com.ecommerce.multivendor.security.JwtTokenProvider;
import com.ecommerce.multivendor.security.SecurityUtils;
import com.ecommerce.multivendor.security.TokenBlacklistService;
import com.ecommerce.multivendor.service.impl.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    /** Register a new CUSTOMER or SELLER. Stages a pending registration until OTP verification. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration initiated. Please verify your email with the OTP sent to " + request.getEmail() + "."));
    }

    /** Authenticate and return JWT tokens */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authService.login(request))
        );
    }

    /** Get a new access token using a refresh token */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7) : bearerToken;
        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed", authService.refreshToken(token))
        );
    }

    /** Verify email using OTP. Creates the user account and returns auth tokens. */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", response));
    }

    /** Resend OTP to email */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam String email) {
        authService.resendVerificationOtp(email);
        return ResponseEntity.ok(ApiResponse.success("OTP resent to " + email));
    }

    /** Send password reset link to email */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Password reset link sent to " + request.getEmail())
        );
    }

    /** Reset password using the token from email */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful. Please login."));
    }

    /** Step 1: request an OTP to change password (authenticated). Verifies current password, emails a 6-digit OTP. */
    @PostMapping("/change-password/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestChangePasswordOtp(
            @Valid @RequestBody ChangePasswordOtpRequest request) {
        authService.requestChangePasswordOtp(securityUtils.getCurrentUser(), request);
        return ResponseEntity.ok(ApiResponse.success("An OTP has been sent to your registered email."));
    }

    /** Step 2: change password (authenticated) — requires current password + the OTP emailed in step 1. */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(securityUtils.getCurrentUser(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        // Blacklist the access token so it can't be used again for the rest of its lifetime.
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7).trim();
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    tokenBlacklistService.blacklistToken(token, jwtTokenProvider.getExpirationDateFromToken(token));
                }
            } catch (Exception e) {
                log.warn("Failed to blacklist token on logout: {}", e.getMessage());
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}


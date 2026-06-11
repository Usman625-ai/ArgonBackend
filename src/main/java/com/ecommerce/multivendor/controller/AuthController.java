package com.ecommerce.multivendor.controller;

import com.ecommerce.multivendor.dto.request.*;
import com.ecommerce.multivendor.dto.response.ApiResponse;
import com.ecommerce.multivendor.dto.response.AuthResponse;
import com.ecommerce.multivendor.security.SecurityUtils;
import com.ecommerce.multivendor.service.impl.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    /** Register a new CUSTOMER or SELLER */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Registration successful. Please verify your email.", response));
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

    /** Verify email using OTP */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
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

    /** Change password (authenticated) */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(securityUtils.getCurrentUser(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT is stateless; client simply discards the token.
        // For production, add a token blacklist (Redis) here.
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}

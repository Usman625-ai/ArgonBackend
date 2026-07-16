package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.*;
import com.ecommerce.multivendor.dto.response.AuthResponse;
import com.ecommerce.multivendor.entity.PasswordResetToken;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.enums.Role;
import com.ecommerce.multivendor.enums.SellerStatus;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.PasswordResetTokenRepository;
import com.ecommerce.multivendor.repository.UserRepository;
import com.ecommerce.multivendor.security.JwtTokenProvider;
import com.ecommerce.multivendor.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // ─── Registration ─────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (request.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin registration is not allowed via this endpoint");
        }

        // Validate shop name for sellers
        if (request.getRole() == Role.SELLER) {
            if (request.getShopName() == null || request.getShopName().trim().isEmpty()) {
                throw new BadRequestException("Shop name is required for sellers");
            }
        }

        // Extract values before building user
        final String shopName = request.getRole() == Role.SELLER ? request.getShopName().trim() : null;
        final String email = request.getEmail().toLowerCase().trim();

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .contactNumber(request.getContactNumber())
                .shopName(shopName)
                .active(true)
                .verified(false)
                .build();

// Sellers start as pending approval
// Alert admins that a new seller needs approval
        if (request.getRole() == Role.SELLER) {
            user.setSellerStatus(SellerStatus.PENDING);
            userRepository.findByRole(Role.ADMIN).forEach(admin ->
                    notificationService.createNotification(admin,
                            shopName + " (" + email + ") registered and is awaiting approval.",
                            "New Seller Pending Approval",
                            com.ecommerce.multivendor.enums.NotificationType.GENERAL,
                            "/admin/sellers"));
        }
        // Generate OTP for email verification
        String otp = OtpGenerator.generateOtp(6);
        user.setVerificationOtp(passwordEncoder.encode(otp));
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));

        user = userRepository.save(user);
        log.info("New user registered: {} [{}]", user.getEmail(), user.getRole());

        // Send verification email asynchronously
        emailService.sendVerificationOtp(user, otp);

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ─── Login ─────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Your account has been disabled. Please contact support.");
        }

        // Warn seller if pending (but still allow login so they can see status)
        if (user.getRole() == Role.SELLER && user.getSellerStatus() == SellerStatus.PENDING) {
            log.info("Pending seller logged in: {}", user.getEmail());
        }

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name());

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ─── Refresh Token ─────────────────────────────────────────────────────

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BadRequestException("Invalid token type");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is disabled");
        }

        String newAccessToken  = jwtTokenProvider.generateAccessToken(email, user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email, user.getRole().name());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // ─── Email Verification ────────────────────────────────────────────────

    public void verifyEmail(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(request.getOtp(), user.getVerificationOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        user.setVerified(true);
        user.setVerificationOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        notificationService.createNotification(user,
                "Welcome to " + user.getName() + "! Your email has been verified.",
                "Account Verified", com.ecommerce.multivendor.enums.NotificationType.GENERAL, null);

        log.info("Email verified for user: {}", user.getEmail());
    }

    // ─── Resend OTP ────────────────────────────────────────────────────────

    public void resendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        String otp = OtpGenerator.generateOtp(6);
        user.setVerificationOtp(passwordEncoder.encode(otp));
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendVerificationOtp(user, otp);
        log.info("OTP resent to: {}", email);
    }

    // ─── Forgot Password ───────────────────────────────────────────────────

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));

        // Invalidate old token if exists
        passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId())
                .ifPresent(t -> {
                    t.setUsed(true);
                    passwordResetTokenRepository.save(t);
                });

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user, token);
        log.info("Password reset email sent to: {}", user.getEmail());
    }

    // ─── Reset Password ────────────────────────────────────────────────────

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("This reset link has already been used");
        }
        if (resetToken.isExpired()) {
            throw new BadRequestException("Reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for: {}", user.getEmail());
    }

    // ─── Change Password ───────────────────────────────────────────────────

    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        log.info("Password changed for user: {}", currentUser.getEmail());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .verified(user.isVerified())
                .sellerApproved(user.getSellerStatus() == SellerStatus.APPROVED)
                .build();
    }


}

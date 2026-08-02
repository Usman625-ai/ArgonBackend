package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.*;
import com.ecommerce.multivendor.dto.response.AuthResponse;
import com.ecommerce.multivendor.entity.ChangePasswordOtp;
import com.ecommerce.multivendor.entity.PasswordResetToken;
import com.ecommerce.multivendor.entity.PendingRegistration;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.enums.Role;
import com.ecommerce.multivendor.enums.SellerStatus;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.ChangePasswordOtpRepository;
import com.ecommerce.multivendor.repository.PasswordResetTokenRepository;
import com.ecommerce.multivendor.repository.PendingRegistrationRepository;
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
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ChangePasswordOtpRepository changePasswordOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final NotificationService notificationService;

    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

    // ─── Registration ─────────────────────────────────────────────────────

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (request.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin registration is not allowed via this endpoint");
        }

        if (request.getRole() == Role.SELLER) {
            if (request.getShopName() == null || request.getShopName().trim().isEmpty()) {
                throw new BadRequestException("Shop name is required for sellers");
            }
        }

        final String email = request.getEmail().toLowerCase().trim();

        // Replace any existing pending registration for this email
        pendingRegistrationRepository.findByEmail(email)
                .ifPresent(pendingRegistrationRepository::delete);
        pendingRegistrationRepository.flush();

        String otp = OtpGenerator.generateOtp(6);
        PendingRegistration pending = PendingRegistration.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .contactNumber(request.getContactNumber())
                .shopName(request.getRole() == Role.SELLER ? request.getShopName().trim() : null)
                .verificationOtp(passwordEncoder.encode(otp))
                .otpExpiry(LocalDateTime.now().plusMinutes(1))
                .lastOtpSentAt(LocalDateTime.now())
                .build();

        pendingRegistrationRepository.save(pending);
        log.info("Pending registration staged for: {}", email);

        emailService.sendVerificationOtpEmail(email, request.getName(), otp);
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

    public AuthResponse verifyEmail(VerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please register again."));

        if (pending.getOtpExpiry() == null || LocalDateTime.now().isAfter(pending.getOtpExpiry())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(request.getOtp(), pending.getVerificationOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        // Only now do we create the real user record
        User user = User.builder()
                .name(pending.getName())
                .email(pending.getEmail())
                .password(pending.getPassword())
                .role(pending.getRole())
                .contactNumber(pending.getContactNumber())
                .shopName(pending.getShopName())
                .active(true)
                .verified(true)
                .build();

        if (user.getRole() == Role.SELLER) {
            user.setSellerStatus(SellerStatus.PENDING);
            final String shopName = user.getShopName();
            final String userEmail = user.getEmail();
            userRepository.findByRole(Role.ADMIN).forEach(admin ->
                    notificationService.createNotification(admin,
                            shopName + " (" + userEmail + ") registered and is awaiting approval.",
                            "New Seller Pending Approval",
                            com.ecommerce.multivendor.enums.NotificationType.GENERAL,
                            "/admin/sellers"));
        }

        user = userRepository.save(user);
        pendingRegistrationRepository.delete(pending);
        pendingRegistrationRepository.flush();

        notificationService.createNotification(user,
                "Welcome " + user.getName() + "! Your email has been verified.",
                "Account Verified", com.ecommerce.multivendor.enums.NotificationType.GENERAL, null);

        log.info("Email verified and user created: {}", user.getEmail());

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ─── Resend OTP ────────────────────────────────────────────────────────

    public void resendVerificationOtp(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please register again."));

        if (pending.getLastOtpSentAt() != null) {
            long secondsSinceLast = java.time.Duration.between(pending.getLastOtpSentAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < OTP_RESEND_COOLDOWN_SECONDS) {
                long wait = OTP_RESEND_COOLDOWN_SECONDS - secondsSinceLast;
                throw new BadRequestException("Please wait " + wait + " second(s) before requesting a new OTP.");
            }
        }

        String otp = OtpGenerator.generateOtp(6);
        pending.setVerificationOtp(passwordEncoder.encode(otp));
        pending.setOtpExpiry(LocalDateTime.now().plusMinutes(1));
        pending.setLastOtpSentAt(LocalDateTime.now());
        pendingRegistrationRepository.save(pending);

        emailService.sendVerificationOtpEmail(normalizedEmail, pending.getName(), otp);
        log.info("OTP resent to: {}", normalizedEmail);
    }

    // ─── Forgot Password ───────────────────────────────────────────────────

    public void forgotPassword(ForgotPasswordRequest request) {
        // SECURITY: do not reveal whether an account exists for this email.
        // Always return normally; only actually send an email if the user exists.
        userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .ifPresent(user -> {
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
                });
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

    // ─── Change Password (OTP-protected) ───────────────────────────────────

    /** Step 1: verify current password, then email a 6-digit OTP the user must supply to actually change it. */
    public void requestChangePasswordOtp(User currentUser, ChangePasswordOtpRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        ChangePasswordOtp existing = changePasswordOtpRepository.findByUserId(currentUser.getId()).orElse(null);
        if (existing != null && existing.getLastSentAt() != null) {
            long secondsSinceLast = java.time.Duration.between(existing.getLastSentAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < OTP_RESEND_COOLDOWN_SECONDS) {
                long wait = OTP_RESEND_COOLDOWN_SECONDS - secondsSinceLast;
                throw new BadRequestException("Please wait " + wait + " second(s) before requesting a new OTP.");
            }
        }

        String otp = OtpGenerator.generateOtp(6);
        ChangePasswordOtp entity = existing != null ? existing : ChangePasswordOtp.builder().user(currentUser).build();
        entity.setOtpHash(passwordEncoder.encode(otp));
        entity.setOtpExpiry(LocalDateTime.now().plusMinutes(1));
        entity.setLastSentAt(LocalDateTime.now());
        changePasswordOtpRepository.save(entity);

        emailService.sendChangePasswordOtpEmail(currentUser.getEmail(), currentUser.getName(), otp);
        log.info("Change-password OTP sent for user: {}", currentUser.getEmail());
    }

    /** Step 2: verify current password again + the OTP, then actually update the password. */
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        ChangePasswordOtp otpEntity = changePasswordOtpRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BadRequestException("No OTP was requested. Please request an OTP first."));

        if (otpEntity.isExpired()) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(request.getOtp(), otpEntity.getOtpHash())) {
            throw new BadRequestException("Invalid OTP");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);

        changePasswordOtpRepository.deleteByUserId(currentUser.getId());

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




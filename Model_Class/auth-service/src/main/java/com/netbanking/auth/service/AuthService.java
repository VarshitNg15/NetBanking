package com.netbanking.auth.service;

import com.netbanking.auth.dto.AuthDtos.*;
import com.netbanking.auth.entity.*;
import com.netbanking.auth.event.AuthEventPublisher;
import com.netbanking.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthUserRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final EmailOtpRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;
    private final AuthEventPublisher eventPublisher;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        AuthUser user = AuthUser.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .customerId("C" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified("N")
                .mfaEnabled("Y")
                .failedLoginAttempts(0)
                .build();
        user = userRepository.save(user);

        UserRole role = UserRole.builder().user(user).roleName(RoleName.CUSTOMER).build();
        roleRepository.save(role);
        user.getRoles().add(role);
        issueOtp(user, OtpPurpose.EMAIL_VERIFICATION);

        eventPublisher.publish("USER_REGISTERED", user.getCustomerId(), user.getCustomerId(),
                Map.of("email", user.getEmail()));

        return new MessageResponse("Registration successful. An email verification OTP has been issued.");
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        AuthUser user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (isLocked(user)) {
            recordLogin(user, LoginStatus.LOCKED, ipAddress, userAgent);
            throw new IllegalStateException("Account is temporarily locked");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new BadCredentialsException("Invalid email or password");
        }

        if ("Y".equals(user.getMfaEnabled())) {
            if (request.otp() == null || request.otp().isBlank()) {
                issueOtp(user, OtpPurpose.LOGIN);
                return new TokenResponse(null, null, "Bearer", 0, user.getCustomerId(),
                        user.getRoles().stream().map(UserRole::getRoleName).map(Enum::name).toList(), true);
            }
            verifyOtpForUser(user, request.otp(), OtpPurpose.LOGIN);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        recordLogin(user, LoginStatus.SUCCESS, ipAddress, userAgent);

        var access = jwtService.generateAccessToken(user);
        String rawRefresh = UUID.randomUUID().toString() + UUID.randomUUID();
        RefreshToken refresh = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHashService.sha256(rawRefresh))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refresh);

        eventPublisher.publish("LOGIN_SUCCESS", user.getCustomerId(), user.getCustomerId(),
                Map.of("email", user.getEmail()));
        return new TokenResponse(access.token(), rawRefresh, "Bearer", access.expiresIn(),
                user.getCustomerId(), access.roles(), false);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHashService.sha256(request.refreshToken()))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token is expired or revoked");
        }

        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        AuthUser user = stored.getUser();
        var access = jwtService.generateAccessToken(user);
        String rawRefresh = UUID.randomUUID().toString() + UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHashService.sha256(rawRefresh))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        return new TokenResponse(access.token(), rawRefresh, "Bearer", access.expiresIn(),
                user.getCustomerId(), access.roles(), false);
    }

    @Transactional
    public MessageResponse logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(tokenHashService.sha256(request.refreshToken()))
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                    recordLogin(token.getUser(), LoginStatus.LOGOUT, null, null);
                });
        return new MessageResponse("Logout successful");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String raw = UUID.randomUUID().toString() + UUID.randomUUID();
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHashService.sha256(raw))
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build());
            eventPublisher.publish("PASSWORD_RESET_REQUESTED", user.getCustomerId(), user.getCustomerId(),
                    Map.of("email", user.getEmail(), "token", raw));
        });
        return new MessageResponse("If the account exists, password reset instructions have been issued.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken stored = passwordResetTokenRepository.findByTokenHash(tokenHashService.sha256(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));
        if (stored.getUsedAt() != null || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token is expired or already used");
        }
        AuthUser user = stored.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        stored.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(stored);
        eventPublisher.publish("PASSWORD_RESET_COMPLETED", user.getCustomerId(), user.getCustomerId(), Map.of());
        return new MessageResponse("Password reset successful");
    }

    @Transactional
    public MessageResponse verifyOtp(VerifyOtpRequest request) {
        AuthUser user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid OTP request"));
        verifyOtpForUser(user, request.otp(), OtpPurpose.valueOf(request.purpose().name()));
        if (request.purpose() == OtpPurposeDto.EMAIL_VERIFICATION) {
            user.setEmailVerified("Y");
            userRepository.save(user);
        }
        return new MessageResponse("OTP verified successfully");
    }

    @Transactional
    public void issueOtp(AuthUser user, OtpPurpose purpose) {
        int code = 100000 + secureRandom.nextInt(900000);
        EmailOtp otp = EmailOtp.builder()
                .user(user)
                .otpHash(passwordEncoder.encode(String.valueOf(code)))
                .otpPurpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();
        otpRepository.save(otp);
        eventPublisher.publish("OTP_ISSUED", user.getCustomerId(), user.getCustomerId(),
                Map.of("email", user.getEmail(), "purpose", purpose.name(), "otp", String.format("%06d", code)));
    }

    private void verifyOtpForUser(AuthUser user, String rawOtp, OtpPurpose purpose) {
        EmailOtp otp = otpRepository.findTopByUserUserIdAndOtpPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(user.getUserId(), purpose)
                .orElseThrow(() -> new IllegalArgumentException("OTP not found"));
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP expired");
        }
        if (otp.getAttemptCount() >= 5) {
            throw new IllegalArgumentException("OTP attempt limit exceeded");
        }
        otp.setAttemptCount(otp.getAttemptCount() + 1);
        if (!passwordEncoder.matches(rawOtp, otp.getOtpHash())) {
            otpRepository.save(otp);
            throw new BadCredentialsException("Invalid OTP");
        }
        otp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otp);
    }

    private boolean isLocked(AuthUser user) {
        return user.getAccountStatus() == AccountStatus.LOCKED
                && user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private void handleFailedLogin(AuthUser user, String ipAddress, String userAgent) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= 5) {
            user.setAccountStatus(AccountStatus.LOCKED);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        }
        userRepository.save(user);
        recordLogin(user, attempts >= 5 ? LoginStatus.LOCKED : LoginStatus.FAILED, ipAddress, userAgent);
    }

    private void recordLogin(AuthUser user, LoginStatus status, String ipAddress, String userAgent) {
        loginHistoryRepository.save(LoginHistory.builder()
                .user(user)
                .loginStatus(status)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build());
    }
}

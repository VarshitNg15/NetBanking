package com.netbanking.auth.service;

import com.netbanking.auth.dto.AuthDtos.LoginRequest;
import com.netbanking.auth.entity.AccountStatus;
import com.netbanking.auth.entity.AuthUser;
import com.netbanking.auth.entity.EmailOtp;
import com.netbanking.auth.entity.OtpPurpose;
import com.netbanking.auth.event.AuthEventPublisher;
import com.netbanking.auth.metrics.AuthMetrics;
import com.netbanking.auth.repository.AuthUserRepository;
import com.netbanking.auth.repository.EmailOtpRepository;
import com.netbanking.auth.repository.LoginHistoryRepository;
import com.netbanking.auth.repository.PasswordResetTokenRepository;
import com.netbanking.auth.repository.RefreshTokenRepository;
import com.netbanking.auth.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private AuthUserRepository userRepository;
    @Mock private UserRoleRepository roleRepository;
    @Mock private EmailOtpRepository otpRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private LoginHistoryRepository loginHistoryRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenHashService tokenHashService;
    @Mock private AuthEventPublisher eventPublisher;
    @Mock private AuthMetrics authMetrics;
    @InjectMocks private AuthService authService;

    @Test
    void locksAccountOnSixthIncorrectPassword() {
        AuthUser user = user("N");
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("password-hash"))).thenReturn(false);

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThrows(BadCredentialsException.class, () -> login(user, null));
            assertEquals(attempt, user.getFailedLoginAttempts());
            assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        }

        LocalDateTime beforeLock = LocalDateTime.now();
        assertThrows(BadCredentialsException.class, () -> login(user, null));

        assertEquals(6, user.getFailedLoginAttempts());
        assertEquals(AccountStatus.LOCKED, user.getAccountStatus());
        assertTrue(user.getLockedUntil().isAfter(beforeLock.plusMinutes(14)));
        assertTrue(user.getLockedUntil().isBefore(beforeLock.plusMinutes(16)));
    }

    @Test
    void locksAccountOnSixthIncorrectLoginOtp() {
        AuthUser user = user("Y");
        EmailOtp otp = EmailOtp.builder()
                .user(user)
                .otpHash("otp-hash")
                .otpPurpose(OtpPurpose.LOGIN)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "password-hash")).thenReturn(true);
        when(passwordEncoder.matches("wrong-otp", "otp-hash")).thenReturn(false);
        when(otpRepository.findTopByUserUserIdAndOtpPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
                user.getUserId(), OtpPurpose.LOGIN)).thenReturn(Optional.of(otp));

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThrows(BadCredentialsException.class, () -> login(user, "wrong-otp"));
            assertEquals(attempt, user.getFailedLoginAttempts());
            assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        }

        LocalDateTime beforeLock = LocalDateTime.now();
        assertThrows(BadCredentialsException.class, () -> login(user, "wrong-otp"));

        assertEquals(6, user.getFailedLoginAttempts());
        assertEquals(6, otp.getAttemptCount());
        assertEquals(AccountStatus.LOCKED, user.getAccountStatus());
        assertTrue(user.getLockedUntil().isAfter(beforeLock.plusMinutes(14)));
        assertTrue(user.getLockedUntil().isBefore(beforeLock.plusMinutes(16)));
    }

    @Test
    void allowsLoginAttemptsAgainAfterLockExpires() {
        AuthUser user = user("N");
        user.setAccountStatus(AccountStatus.LOCKED);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        user.setFailedLoginAttempts(6);
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("password-hash"))).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> login(user, null));

        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        assertEquals(1, user.getFailedLoginAttempts());
        assertEquals(null, user.getLockedUntil());
    }

    private void login(AuthUser user, String otp) {
        authService.login(new LoginRequest(user.getEmail(), "correct-password", otp), "127.0.0.1", "test-agent");
    }

    private AuthUser user(String mfaEnabled) {
        return AuthUser.builder()
                .userId(1L)
                .email("customer@example.com")
                .passwordHash("password-hash")
                .accountStatus(AccountStatus.ACTIVE)
                .mfaEnabled(mfaEnabled)
                .failedLoginAttempts(0)
                .build();
    }
}
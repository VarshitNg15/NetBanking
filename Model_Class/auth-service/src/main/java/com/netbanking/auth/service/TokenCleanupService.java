package com.netbanking.auth.service;

import com.netbanking.auth.repository.EmailOtpRepository;
import com.netbanking.auth.repository.PasswordResetTokenRepository;
import com.netbanking.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenCleanupService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailOtpRepository emailOtpRepository;

    @Scheduled(fixedDelayString = "${app.cleanup.delay-ms:3600000}")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findAll().stream()
                .filter(token -> token.getExpiresAt().isBefore(now) || token.getRevokedAt() != null)
                .forEach(refreshTokenRepository::delete);
        passwordResetTokenRepository.findAll().stream()
                .filter(token -> token.getExpiresAt().isBefore(now) || token.getUsedAt() != null)
                .forEach(passwordResetTokenRepository::delete);
        emailOtpRepository.findAll().stream()
                .filter(otp -> otp.getExpiresAt().isBefore(now) || otp.getVerifiedAt() != null)
                .forEach(emailOtpRepository::delete);
    }
}

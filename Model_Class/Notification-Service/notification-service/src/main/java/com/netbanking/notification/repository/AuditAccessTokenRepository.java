package com.netbanking.notification.repository;

import com.netbanking.notification.entity.AuditAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditAccessTokenRepository extends JpaRepository<AuditAccessToken, Long> {
    Optional<AuditAccessToken> findByTokenHash(String tokenHash);
}

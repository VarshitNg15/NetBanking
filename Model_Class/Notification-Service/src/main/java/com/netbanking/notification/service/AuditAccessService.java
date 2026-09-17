package com.netbanking.notification.service;

import com.netbanking.notification.dto.AuditAccessRequestDto;
import com.netbanking.notification.entity.AuditAccessRequest;
import com.netbanking.notification.entity.AuditAccessRequestStatus;
import com.netbanking.notification.entity.AuditAccessToken;
import com.netbanking.notification.entity.AuditAccessTokenStatus;
import com.netbanking.notification.repository.AuditAccessRequestRepository;
import com.netbanking.notification.repository.AuditAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditAccessService {

    private final AuditAccessRequestRepository requestRepository;
    private final AuditAccessTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuditAccessRequest requestAccess(AuditAccessRequestDto dto) {
        AuditAccessRequest request = AuditAccessRequest.builder()
                .adminId(dto.adminId())
                .status(AuditAccessRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .reason(dto.reason())
                .build();

        return requestRepository.save(request);
    }

    @Transactional
    public String approve(Long requestId, String approvedBy) {
        AuditAccessRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Audit access request not found: " + requestId));

        request.setStatus(AuditAccessRequestStatus.APPROVED);
        request.setApprovedBy(approvedBy);
        request.setApprovedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        String rawToken = UUID.randomUUID().toString();
        AuditAccessToken token = AuditAccessToken.builder()
                .accessRequest(request)
                .tokenHash(passwordEncoder.encode(rawToken))
                .issuedAt(LocalDateTime.now())
                .expiresAt(request.getExpiresAt())
                .status(AuditAccessTokenStatus.ACTIVE)
                .build();

        requestRepository.save(request);
        tokenRepository.save(token);
        return rawToken;
    }
}

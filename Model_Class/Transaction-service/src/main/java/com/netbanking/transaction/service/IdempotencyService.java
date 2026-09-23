package com.netbanking.transaction.service;

import com.netbanking.transaction.entity.IdempotencyRecord;
import com.netbanking.transaction.exception.DuplicateRequestException;
import com.netbanking.transaction.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    @Transactional(readOnly = true)
    public IdempotencyRecord find(String key) {
        return repository.findByIdempotencyKey(key).orElse(null);
    }

    @Transactional
    public IdempotencyRecord createProcessingRecord(String key, String customerId, String requestHash) {
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdempotencyKey(key);
            record.setCustomerId(customerId);
            record.setRequestHash(requestHash);
            record.setStatus("PROCESSING");
            record.setCreatedAt(LocalDateTime.now());
            record.setExpiresAt(LocalDateTime.now().plusHours(24));
            return repository.save(record);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate idempotency key detected: {}", key);
            throw new DuplicateRequestException("Concurrent request with idempotency key: " + key);
        }
    }

    @Transactional
    public void complete(IdempotencyRecord record, String status) {
        record.setStatus(status);
        repository.save(record);
    }

    public String computeHash(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(payload.hashCode());
        }
    }
}

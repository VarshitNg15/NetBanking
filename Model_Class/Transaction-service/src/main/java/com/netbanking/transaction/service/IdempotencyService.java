package com.netbanking.transaction.service;

import com.netbanking.transaction.entity.IdempotencyRecord;
import com.netbanking.transaction.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    @Transactional(readOnly = true)
    public IdempotencyRecord find(String key) {
        return repository.findByIdempotencyKey(key).orElse(null);
    }

    @Transactional
    public IdempotencyRecord createProcessingRecord(String key, String customerId, String requestHash) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setCustomerId(customerId);
        record.setRequestHash(requestHash);
        record.setStatus("PROCESSING");
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusHours(24));
        return repository.save(record);
    }

    @Transactional
    public void complete(IdempotencyRecord record, String status) {
        record.setStatus(status);
        repository.save(record);
    }
}

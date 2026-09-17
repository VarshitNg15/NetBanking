package com.netbanking.notification.service;

import com.netbanking.notification.entity.AuditLog;
import com.netbanking.notification.exception.DuplicateEventException;
import com.netbanking.notification.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog record(AuditLog auditLog) {
        if (auditLog.getEventId() != null && auditLogRepository.findByEventId(auditLog.getEventId()).isPresent()) {
            throw new DuplicateEventException("Audit event already processed: " + auditLog.getEventId());
        }

        if (auditLog.getEventTimestamp() == null) {
            auditLog.setEventTimestamp(LocalDateTime.now());
        }

        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findByCustomer(String customerId) {
        return auditLogRepository.findByCustomerIdOrderByEventTimestampDesc(customerId);
    }
}

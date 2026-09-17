package com.netbanking.notification.repository;

import com.netbanking.notification.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findByEventId(String eventId);
    List<AuditLog> findByCustomerIdOrderByEventTimestampDesc(String customerId);
    List<AuditLog> findByActorIdOrderByEventTimestampDesc(String actorId);
}

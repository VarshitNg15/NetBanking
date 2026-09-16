package com.netbanking.notification.repository;

import com.netbanking.notification.entity.AuditAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditAccessRequestRepository extends JpaRepository<AuditAccessRequest, Long> {
    List<AuditAccessRequest> findByAdminIdOrderByRequestedAtDesc(String adminId);
}

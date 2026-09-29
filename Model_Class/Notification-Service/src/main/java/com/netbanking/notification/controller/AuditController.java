package com.netbanking.notification.controller;

import com.netbanking.notification.dto.AuditAccessApprovalDto;
import com.netbanking.notification.dto.AuditAccessRequestDto;
import com.netbanking.notification.entity.AuditAccessRequest;
import com.netbanking.notification.service.AuditAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/audit/access", "/api/audit/access"})
@RequiredArgsConstructor
@Tag(name = "Audit Log Access", description = "Endpoints for requesting and approving audit trail access tokens")
public class AuditController {

    private final AuditAccessService auditAccessService;
    private final com.netbanking.notification.metrics.NotificationMetrics metrics;

    @PostMapping("/request")
    public ResponseEntity<AuditAccessRequest> requestAccess(
            @Valid @RequestBody AuditAccessRequestDto request) {
        try {
            AuditAccessRequest resp = auditAccessService.requestAccess(request);
            metrics.recordAuditAccess("REQUEST", "SUCCESS");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            metrics.recordAuditAccess("REQUEST", "FAILURE");
            throw e;
        }
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable Long requestId,
            @Valid @RequestBody AuditAccessApprovalDto request) {
        try {
            String token = auditAccessService.approve(requestId, request.approvedBy());
            metrics.recordAuditAccess("APPROVE", "SUCCESS");
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            metrics.recordAuditAccess("APPROVE", "FAILURE");
            throw e;
        }
    }
}

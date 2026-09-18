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

    @PostMapping("/request")
    public ResponseEntity<AuditAccessRequest> requestAccess(
            @Valid @RequestBody AuditAccessRequestDto request) {
        return ResponseEntity.ok(auditAccessService.requestAccess(request));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable Long requestId,
            @Valid @RequestBody AuditAccessApprovalDto request) {
        String token = auditAccessService.approve(requestId, request.approvedBy());
        return ResponseEntity.ok(Map.of("token", token));
    }
}

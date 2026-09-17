package com.netbanking.notification.controller;

import com.netbanking.notification.dto.AuditAccessApprovalDto;
import com.netbanking.notification.dto.AuditAccessRequestDto;
import com.netbanking.notification.entity.AuditAccessRequest;
import com.netbanking.notification.service.AuditAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit/access")
@RequiredArgsConstructor
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

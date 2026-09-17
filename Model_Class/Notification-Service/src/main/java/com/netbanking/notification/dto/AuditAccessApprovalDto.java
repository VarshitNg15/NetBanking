package com.netbanking.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditAccessApprovalDto(
        @NotBlank String approvedBy
) {}

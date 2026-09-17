package com.netbanking.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditAccessRequestDto(
        @NotBlank String adminId,
        String reason
) {}

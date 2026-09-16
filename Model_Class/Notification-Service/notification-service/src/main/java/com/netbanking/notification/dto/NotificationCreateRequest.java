package com.netbanking.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotificationCreateRequest(
        @NotBlank String eventId,
        @NotBlank String eventType,
        @NotBlank String customerId,
        @NotBlank @Email String recipientEmail,
        @NotBlank String subject,
        @NotBlank String messageBody
) {}

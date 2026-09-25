package com.netbanking.user_service.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOpeningResponseDto {

    private String requestId;

    private String customerId;

    private String customerName;

    private String requestStatus;

    private List<String> accountTypes;

    private LocalDateTime submittedAt;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
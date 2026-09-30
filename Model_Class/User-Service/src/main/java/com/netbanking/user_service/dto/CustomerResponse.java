package com.netbanking.user_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private String customerId;

    private String customerName;

    private String customerStatus;

    private Boolean kycCompleted;

    private String kycStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
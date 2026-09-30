package com.netbanking.user_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerKycResponse {

    private String customerId;

    private boolean kycCompleted;

    private String kycStatus;

    private String message;
}

package com.netbanking.user_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreationResponse {

    private Long accountId;

    private String customerId;

    private String accountNumber;

    private String accountType;

    private String accountStatus;

    private String currencyCode;
}
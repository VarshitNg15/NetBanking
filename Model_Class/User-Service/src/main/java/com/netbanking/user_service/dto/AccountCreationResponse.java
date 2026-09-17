package com.netbanking.user_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountCreationResponse {

    @JsonAlias({"id", "accountId"})
    private Long accountId;

    private String customerId;

    private String accountNumber;

    private String accountType;

    @JsonAlias({"status", "accountStatus"})
    private String accountStatus;

    private String currencyCode;
}
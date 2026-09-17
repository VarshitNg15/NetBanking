package com.netbanking.transaction.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "account-service", url = "${clients.account-service.url:http://localhost:8083}")
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{accountId}")
    AccountResponse getAccount(@PathVariable("accountId") Long accountId);

    @PostMapping("/api/accounts/{accountId}/debit")
    BalanceOperationResponse debit(@PathVariable("accountId") Long accountId, @RequestBody BalanceOperationRequest request);

    @PostMapping("/api/accounts/{accountId}/credit")
    BalanceOperationResponse credit(@PathVariable("accountId") Long accountId, @RequestBody BalanceOperationRequest request);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountResponse(
            @JsonAlias({"id", "accountId"}) Long accountId,
            String customerId,
            @JsonAlias({"status", "accountStatus"}) String accountStatus,
            String currencyCode
    ) {}

    record BalanceOperationRequest(BigDecimal amount, String transactionReference, String description, String initiatedBy) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BalanceOperationResponse(boolean successful, String message) {}
}

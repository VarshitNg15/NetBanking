package com.netbanking.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "account-service", url = "${clients.account-service.url:http://localhost:8082}")
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{accountId}")
    AccountResponse getAccount(@PathVariable Long accountId);

    @PostMapping("/api/accounts/{accountId}/debit")
    BalanceOperationResponse debit(@PathVariable Long accountId, @RequestBody BalanceOperationRequest request);

    @PostMapping("/api/accounts/{accountId}/credit")
    BalanceOperationResponse credit(@PathVariable Long accountId, @RequestBody BalanceOperationRequest request);

    record AccountResponse(Long accountId, String customerId, String accountStatus, String currencyCode) {}
    record BalanceOperationRequest(BigDecimal amount, String transactionReference, String description, String initiatedBy) {}
    record BalanceOperationResponse(boolean successful, String message) {}
}

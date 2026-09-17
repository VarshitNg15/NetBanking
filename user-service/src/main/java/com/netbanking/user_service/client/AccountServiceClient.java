package com.netbanking.user_service.client;

import com.netbanking.user_service.dto.AccountCreationRequest;
import com.netbanking.user_service.dto.AccountCreationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "account-service",
        url = "${services.account-service.url}"
)
public interface AccountServiceClient {

    @PostMapping("/api/accounts")
    AccountCreationResponse createAccount(
            @RequestBody AccountCreationRequest request
    );
}
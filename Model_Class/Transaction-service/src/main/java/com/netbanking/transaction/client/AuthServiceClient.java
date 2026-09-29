package com.netbanking.transaction.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${clients.auth-service.url:http://localhost:8081}")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{customerId}")
    UserSummaryResponse getUserByCustomerId(@PathVariable("customerId") String customerId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserSummaryResponse(
            String customerId,
            String email,
            String status
    ) {}
}

package com.netbanking.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${clients.user-service.url:http://localhost:8082}")
public interface CustomerServiceClient {

    @PostMapping("/api/v1/customers")
    CustomerResponse createCustomer(@RequestBody CustomerCreateRequest request);

    @GetMapping("/api/v1/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable("customerId") String customerId);

    record CustomerCreateRequest(String customerId, String email, String customerStatus) {}
    record CustomerResponse(String customerId, String customerStatus) {}
}

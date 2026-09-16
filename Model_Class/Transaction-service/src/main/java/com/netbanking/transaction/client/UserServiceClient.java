package com.netbanking.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${clients.user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    @GetMapping("/api/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable String customerId);

    record CustomerResponse(String customerId, String customerStatus) {}
}

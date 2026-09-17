package com.netbanking.transaction.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${clients.user-service.url:http://localhost:8082}")
public interface UserServiceClient {

    @GetMapping("/api/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable("customerId") String customerId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CustomerResponse(
            String customerId,
            @JsonAlias({"customerStatus", "status"}) String customerStatus
    ) {}
}

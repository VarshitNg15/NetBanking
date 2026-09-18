package com.netbanking.user_service.controller;

import com.netbanking.user_service.dto.CustomerRequest;
import com.netbanking.user_service.dto.CustomerResponse;
import com.netbanking.user_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/customers", "/api/users/customers", "/api/customers"})
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Endpoints for managing customer identity and status")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request
    ) {

        CustomerResponse response =
                customerService.createCustomer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable String customerId
    ) {

        return ResponseEntity.ok(
                customerService.getCustomerById(customerId)
        );
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {

        return ResponseEntity.ok(
                customerService.getAllCustomers()
        );
    }

    @PatchMapping("/{customerId}/status")
    public ResponseEntity<CustomerResponse> updateCustomerStatus(
            @PathVariable String customerId,
            @RequestParam String status
    ) {

        return ResponseEntity.ok(
                customerService.updateCustomerStatus(
                        customerId,
                        status
                )
        );
    }
}
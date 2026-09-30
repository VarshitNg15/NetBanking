package com.netbanking.user_service.controller;

import com.netbanking.user_service.dto.CustomerProfileRequest;
import com.netbanking.user_service.dto.CustomerProfileResponse;
import com.netbanking.user_service.dto.CustomerKycResponse;
import com.netbanking.user_service.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/customers", "/api/users/customers", "/api/customers"})
@RequiredArgsConstructor
@Tag(name = "Customer Profile", description = "Endpoints for managing KYC and customer profile information")
public class ProfileController {

    private final CustomerProfileService customerProfileService;

    @PostMapping("/{customerId}/profile")
    public ResponseEntity<CustomerProfileResponse> createProfile(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerProfileRequest request
    ) {

        CustomerProfileResponse response =
                customerProfileService.createProfile(
                        customerId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{customerId}/profile")
    public ResponseEntity<CustomerProfileResponse> getProfile(
            @PathVariable String customerId
    ) {

        return ResponseEntity.ok(
                customerProfileService.getProfile(customerId)
        );
    }

    @GetMapping("/{customerId}/kyc")
    @Operation(summary = "Get KYC compliance status", description = "Verifies whether the customer has completed mandatory profile setup required for banking operations")
    public ResponseEntity<CustomerKycResponse> getKycStatus(
            @PathVariable String customerId
    ) {
        boolean completed = customerProfileService.isKycCompleted(customerId);
        return ResponseEntity.ok(
                CustomerKycResponse.builder()
                        .customerId(customerId)
                        .kycCompleted(completed)
                        .kycStatus(completed ? "COMPLETED" : "PENDING")
                        .message(completed ? "Customer profile KYC is completed" : "Customer profile KYC setup is pending")
                        .build()
        );
    }

    @PutMapping("/{customerId}/profile")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerProfileRequest request
    ) {

        return ResponseEntity.ok(
                customerProfileService.updateProfile(
                        customerId,
                        request
                )
        );
    }
}
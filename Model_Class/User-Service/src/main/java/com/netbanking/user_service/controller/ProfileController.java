package com.netbanking.user_service.controller;

import com.netbanking.user_service.dto.CustomerProfileRequest;
import com.netbanking.user_service.dto.CustomerProfileResponse;
import com.netbanking.user_service.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/customers", "/api/users/customers", "/api/customers"})
@RequiredArgsConstructor
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
package com.netbanking.user_service.controller;

import com.netbanking.user_service.dto.AccountOpeningRequestDto;
import com.netbanking.user_service.dto.AccountOpeningResponseDto;
import com.netbanking.user_service.service.AccountOpeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/users/account-opening", "/api/users/account-opening", "/api/v1/customers/account-requests"})
@RequiredArgsConstructor
@Tag(name = "Account Opening Requests", description = "Endpoints for customer account opening requests")
public class AccountOpeningController {

    private final AccountOpeningService accountOpeningService;

    @PostMapping
    public ResponseEntity<AccountOpeningResponseDto> createRequest(
            @Valid @RequestBody AccountOpeningRequestDto request
    ) {

        AccountOpeningResponseDto response =
                accountOpeningService.createRequest(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<AccountOpeningResponseDto> getRequest(
            @PathVariable String requestId
    ) {

        return ResponseEntity.ok(
                accountOpeningService.getRequest(requestId)
        );
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountOpeningResponseDto>>
    getCustomerRequests(
            @PathVariable String customerId
    ) {

        return ResponseEntity.ok(
                accountOpeningService.getCustomerRequests(
                        customerId
                )
        );
    }
}
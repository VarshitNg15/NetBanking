package com.netbanking.user_service.controller;

import com.netbanking.user_service.dto.AccountApprovalRequest;
import com.netbanking.user_service.dto.AccountOpeningResponseDto;
import com.netbanking.user_service.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/account-opening/pending")
    public ResponseEntity<List<AccountOpeningResponseDto>>
    getPendingAccountOpeningRequests() {

        return ResponseEntity.ok(
                adminService.getPendingAccountRequests()
        );
    }

    @PostMapping("/account-opening/{requestId}/approve")
    public ResponseEntity<AccountOpeningResponseDto>
    approveAccountOpening(
            @PathVariable String requestId,
            @Valid @RequestBody AccountApprovalRequest request
    ) {

        return ResponseEntity.ok(
                adminService.approveAccountOpening(
                        requestId,
                        request
                )
        );
    }

    @PostMapping("/account-opening/{requestId}/reject")
    public ResponseEntity<AccountOpeningResponseDto>
    rejectAccountOpening(
            @PathVariable String requestId,
            @Valid @RequestBody AccountApprovalRequest request
    ) {

        return ResponseEntity.ok(
                adminService.rejectAccountOpening(
                        requestId,
                        request
                )
        );
    }
}
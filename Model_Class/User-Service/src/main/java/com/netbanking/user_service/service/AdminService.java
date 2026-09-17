package com.netbanking.user_service.service;

import com.netbanking.user_service.dto.AccountApprovalRequest;
import com.netbanking.user_service.dto.AccountOpeningResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountOpeningService accountOpeningService;

    @Transactional(readOnly = true)
    public List<AccountOpeningResponseDto> getPendingAccountRequests() {

        return accountOpeningService.getPendingRequests();
    }

    @Transactional
    public AccountOpeningResponseDto approveAccountOpening(
            String requestId,
            AccountApprovalRequest request
    ) {

        return accountOpeningService.approveRequest(
                requestId,
                request.getAdminId()
        );
    }

    @Transactional
    public AccountOpeningResponseDto rejectAccountOpening(
            String requestId,
            AccountApprovalRequest request
    ) {

        return accountOpeningService.rejectRequest(
                requestId,
                request.getAdminId(),
                request.getReason()
        );
    }
}
package com.netbanking.user_service.service;

import com.netbanking.user_service.dto.AccountOpeningRequestDto;
import com.netbanking.user_service.dto.AccountOpeningResponseDto;
import com.netbanking.user_service.entity.AccountOpeningRequest;
import com.netbanking.user_service.entity.AccountOpeningRequestType;
import com.netbanking.user_service.entity.Customer;
import com.netbanking.user_service.client.AccountServiceClient;
import com.netbanking.user_service.repository.AccountOpeningRequestRepository;
import com.netbanking.user_service.repository.AccountOpeningRequestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountOpeningService {

    private final AccountOpeningRequestRepository requestRepository;

    private final AccountOpeningRequestTypeRepository requestTypeRepository;

    private final AccountServiceClient accountServiceClient;
    
    private final CustomerService customerService;

    public AccountOpeningResponseDto createRequest(
            AccountOpeningRequestDto request
    ) {

        Customer customer =
                customerService.getCustomerEntityForInternalUse(
                        request.getCustomerId()
                );

        if (requestRepository.existsById(request.getRequestId())) {
            throw new IllegalArgumentException(
                    "Request already exists: "
                            + request.getRequestId()
            );
        }

        validateAccountTypes(request.getAccountTypes());

        AccountOpeningRequest openingRequest =
                new AccountOpeningRequest();

        openingRequest.setRequestId(request.getRequestId());
        openingRequest.setCustomer(customer);
        openingRequest.setRequestStatus("PENDING");

        LocalDateTime now = LocalDateTime.now();

        openingRequest.setSubmittedAt(now);
        openingRequest.setCreatedAt(now);
        openingRequest.setUpdatedAt(now);

        AccountOpeningRequest savedRequest =
                requestRepository.save(openingRequest);

        for (String type : request.getAccountTypes()) {

            AccountOpeningRequestType requestType =
                    new AccountOpeningRequestType();

            requestType.setAccountOpeningRequest(savedRequest);
            requestType.setAccountType(type);

            requestTypeRepository.save(requestType);
        }

        return toResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public AccountOpeningResponseDto getRequest(
            String requestId
    ) {

        AccountOpeningRequest request =
                requestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Account opening request not found: "
                                                + requestId
                                )
                        );

        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<AccountOpeningResponseDto> getCustomerRequests(
            String customerId
    ) {

        return requestRepository
                .findByCustomerCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountOpeningResponseDto> getPendingRequests() {

        return requestRepository
                .findByRequestStatus("PENDING")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountOpeningResponseDto approveRequest(
            String requestId,
            String adminId
    ) {

        AccountOpeningRequest request =
                getRequestEntity(requestId);

        ensurePending(request);

        request.setRequestStatus("APPROVED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        return toResponse(
                requestRepository.save(request)
        );
    }

    public AccountOpeningResponseDto rejectRequest(
            String requestId,
            String adminId,
            String reason
    ) {

        AccountOpeningRequest request =
                getRequestEntity(requestId);

        ensurePending(request);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required"
            );
        }

        request.setRequestStatus("REJECTED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(reason);
        request.setUpdatedAt(LocalDateTime.now());

        return toResponse(
                requestRepository.save(request)
        );
    }

    private AccountOpeningRequest getRequestEntity(
            String requestId
    ) {

        return requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account opening request not found: "
                                        + requestId
                        )
                );
    }

    private void ensurePending(
            AccountOpeningRequest request
    ) {

        if (!"PENDING".equals(request.getRequestStatus())) {
            throw new IllegalStateException(
                    "Request is already "
                            + request.getRequestStatus()
            );
        }
    }

    private void validateAccountTypes(
            List<String> accountTypes
    ) {

        if (accountTypes == null || accountTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one account type is required"
            );
        }

        for (String type : accountTypes) {

            if (!"SAVINGS".equals(type)
                    && !"CURRENT".equals(type)) {

                throw new IllegalArgumentException(
                        "Invalid account type: " + type
                );
            }
        }
    }

    private AccountOpeningResponseDto toResponse(
            AccountOpeningRequest request
    ) {

        List<String> accountTypes =
                requestTypeRepository
                        .findByAccountOpeningRequestRequestId(
                                request.getRequestId()
                        )
                        .stream()
                        .map(AccountOpeningRequestType::getAccountType)
                        .toList();

        return AccountOpeningResponseDto.builder()
                .requestId(request.getRequestId())
                .customerId(
                        request.getCustomer().getCustomerId()
                )
                .requestStatus(request.getRequestStatus())
                .accountTypes(accountTypes)
                .submittedAt(request.getSubmittedAt())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
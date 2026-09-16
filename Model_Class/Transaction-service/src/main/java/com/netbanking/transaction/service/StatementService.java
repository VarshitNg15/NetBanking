package com.netbanking.transaction.service;

import com.netbanking.transaction.dto.request.StatementRequestDto;
import com.netbanking.transaction.dto.response.StatementResponse;
import com.netbanking.transaction.entity.StatementRequest;
import com.netbanking.transaction.repository.StatementRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRequestRepository repository;

    @Transactional
    public StatementResponse requestStatement(StatementRequestDto request, String customerId, String requestedBy) {
        StatementRequest entity = new StatementRequest();
        entity.setAccountId(request.accountId());
        entity.setCustomerId(customerId);
        entity.setRequestedBy(requestedBy);
        entity.setRequestType(request.requestType());
        entity.setFromDate(request.fromDate());
        entity.setToDate(request.toDate());
        entity.setStatus("PROCESSING");
        entity.setRequestedAt(LocalDateTime.now());
        entity = repository.save(entity);

        // Actual file generation and Account Service ledger retrieval are intentionally
        // left as the next integration step for the project.
        return toResponse(entity);
    }

    private StatementResponse toResponse(StatementRequest entity) {
        return new StatementResponse(
                entity.getRequestId(),
                entity.getAccountId(),
                entity.getCustomerId(),
                entity.getRequestType(),
                entity.getStatus(),
                entity.getFileName(),
                entity.getFileUrl(),
                entity.getRequestedAt(),
                entity.getCompletedAt(),
                entity.getFailureReason()
        );
    }
}

package com.netbanking.transaction.service;

import com.netbanking.transaction.client.AccountServiceClient;
import com.netbanking.transaction.dto.request.StatementRequestDto;
import com.netbanking.transaction.dto.response.StatementResponse;
import com.netbanking.transaction.entity.StatementRequest;
import com.netbanking.transaction.exception.ResourceNotFoundException;
import com.netbanking.transaction.repository.StatementRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final StatementRequestRepository repository;
    private final AccountServiceClient accountServiceClient;

    @Transactional
    public StatementResponse requestStatement(StatementRequestDto request, String customerId, String requestedBy) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID header (X-Customer-Id) is required");
        }
        if (requestedBy == null || requestedBy.isBlank()) {
            requestedBy = "CUSTOMER";
        }

        // 1. Verify account ownership
        try {
            AccountServiceClient.AccountResponse account = accountServiceClient.getAccount(request.accountId());
            if (account != null && account.customerId() != null && !account.customerId().equalsIgnoreCase(customerId)) {
                throw new SecurityException("Unauthorized: Account " + request.accountId() + " does not belong to customer " + customerId);
            }
        } catch (Exception ex) {
            log.warn("Could not verify account ownership for statement request: {}", ex.getMessage());
        }

        String type = request.normalizedRequestType();
        LocalDateTime from = request.fromDate() != null ? request.fromDate() : LocalDateTime.now().minusDays(30);
        LocalDateTime to = request.toDate() != null ? request.toDate() : LocalDateTime.now();

        StatementRequest entity = new StatementRequest();
        entity.setAccountId(request.accountId());
        entity.setCustomerId(customerId);
        entity.setRequestedBy(requestedBy);
        entity.setRequestType(type);
        entity.setFromDate(from);
        entity.setToDate(to);
        entity.setStatus("PROCESSING");
        entity.setRequestedAt(LocalDateTime.now());
        entity = repository.save(entity);

        // 2. Fetch ledger from Account Service and generate statement
        try {
            List<AccountServiceClient.LedgerEntryResponse> ledgerEntries =
                    accountServiceClient.getLedger(request.accountId());
            if (ledgerEntries == null) {
                ledgerEntries = Collections.emptyList();
            }

            // Filter entries by date range
            List<AccountServiceClient.LedgerEntryResponse> filtered = ledgerEntries.stream()
                    .filter(e -> e.createdAt() != null && !e.createdAt().isBefore(from) && !e.createdAt().isAfter(to))
                    .toList();

            String ext = "CSV".equalsIgnoreCase(type) ? ".csv" : ".txt";
            String fileName = "statement-" + request.accountId() + "-" + entity.getRequestId() + ext;
            String downloadUrl = "/api/v1/statements/" + entity.getRequestId() + "/download";

            entity.setFileName(fileName);
            entity.setFileUrl(downloadUrl);
            entity.setStatus("COMPLETED");
            entity.setCompletedAt(LocalDateTime.now());
            entity = repository.save(entity);

            log.info("Statement generated successfully for requestId={}, accountId={}, totalEntries={}",
                    entity.getRequestId(), request.accountId(), filtered.size());

        } catch (Exception ex) {
            log.error("Failed to generate statement for account {}: {}", request.accountId(), ex.getMessage());
            entity.setStatus("FAILED");
            entity.setFailureReason("Failed to retrieve ledger: " + ex.getMessage());
            entity.setCompletedAt(LocalDateTime.now());
            entity = repository.save(entity);
        }

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public StatementResponse getStatement(Long requestId, String customerId) {
        return (customerId != null && !customerId.isBlank()
                ? repository.findByRequestIdAndCustomerId(requestId, customerId)
                : java.util.Optional.<StatementRequest>empty())
                .or(() -> repository.findById(requestId))
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Statement request not found: " + requestId));
    }

    @Transactional(readOnly = true)
    public List<StatementResponse> getStatementsByCustomer(String customerId) {
        return repository.findByCustomerIdOrderByRequestedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public String generateDownloadContent(Long requestId, String customerId) {
        StatementRequest statement = (customerId != null && !customerId.isBlank()
                ? repository.findByRequestIdAndCustomerId(requestId, customerId)
                : java.util.Optional.<StatementRequest>empty())
                .or(() -> repository.findById(requestId))
                .orElseThrow(() -> new ResourceNotFoundException("Statement request not found: " + requestId));

        List<AccountServiceClient.LedgerEntryResponse> ledgerEntries = Collections.emptyList();
        try {
            ledgerEntries = accountServiceClient.getLedger(statement.getAccountId());
        } catch (Exception e) {
            log.warn("Could not reload ledger entries for download: {}", e.getMessage());
        }

        LocalDateTime from = statement.getFromDate();
        LocalDateTime to = statement.getToDate();

        List<AccountServiceClient.LedgerEntryResponse> filtered = ledgerEntries != null ? ledgerEntries.stream()
                .filter(e -> e.createdAt() != null &&
                        (from == null || !e.createdAt().isBefore(from)) &&
                        (to == null || !e.createdAt().isAfter(to)))
                .toList() : Collections.emptyList();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if ("CSV".equalsIgnoreCase(statement.getRequestType())) {
            StringBuilder sb = new StringBuilder();
            sb.append("TRANSACTION_REFERENCE,ENTRY_REFERENCE,ENTRY_TYPE,AMOUNT,BALANCE_BEFORE,BALANCE_AFTER,AVAILABLE_BALANCE_BEFORE,AVAILABLE_BALANCE_AFTER,DESCRIPTION,CREATED_BY\n");
            for (AccountServiceClient.LedgerEntryResponse e : filtered) {
                sb.append(e.transactionReference() != null ? e.transactionReference() : "").append(",")
                  .append(e.entryReference() != null ? e.entryReference() : "").append(",")
                  .append(e.entryType() != null ? e.entryType() : "").append(",")
                  .append(e.amount() != null ? e.amount() : "0.00").append(",")
                  .append(e.balanceBefore() != null ? e.balanceBefore() : "0.00").append(",")
                  .append(e.balanceAfter() != null ? e.balanceAfter() : "0.00").append(",")
                  .append(e.availableBalanceBefore() != null ? e.availableBalanceBefore() : "0.00").append(",")
                  .append(e.availableBalanceAfter() != null ? e.availableBalanceAfter() : "0.00").append(",")
                  .append(e.description() != null ? "\"" + e.description().replace("\"", "\"\"") + "\"" : "").append(",")
                  .append(e.createdBy() != null ? e.createdBy() : "")
                  .append("\n");
            }
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("========================================================================================================================\n");
            sb.append("                                            NETBANKING ACCOUNT LEDGER STATEMENT                                         \n");
            sb.append("========================================================================================================================\n");
            sb.append("Account ID: ").append(statement.getAccountId()).append("\n");
            sb.append("Customer ID: ").append(statement.getCustomerId()).append("\n");
            sb.append("Period: ").append(from != null ? from.format(dtf) : "Beginning")
              .append(" to ").append(to != null ? to.format(dtf) : "Present").append("\n");
            sb.append("Generated At: ").append(statement.getCompletedAt() != null ? statement.getCompletedAt().format(dtf) : "").append("\n");
            sb.append("------------------------------------------------------------------------------------------------------------------------\n");
            sb.append(String.format("%-18s %-20s %-8s %-12s %-14s %-14s %-14s %-14s %-18s %-12s\n",
                    "TXN_REF", "ENTRY_REF", "TYPE", "AMOUNT", "BAL_BEFORE", "BAL_AFTER", "AVAIL_BEFORE", "AVAIL_AFTER", "DESCRIPTION", "CREATED_BY"));
            sb.append("------------------------------------------------------------------------------------------------------------------------\n");
            for (AccountServiceClient.LedgerEntryResponse e : filtered) {
                sb.append(String.format("%-18s %-20s %-8s %-12s %-14s %-14s %-14s %-14s %-18s %-12s\n",
                        e.transactionReference() != null ? e.transactionReference() : "-",
                        e.entryReference() != null ? e.entryReference() : "-",
                        e.entryType() != null ? e.entryType() : "-",
                        e.amount() != null ? e.amount().toString() : "0.00",
                        e.balanceBefore() != null ? e.balanceBefore().toString() : "0.00",
                        e.balanceAfter() != null ? e.balanceAfter().toString() : "0.00",
                        e.availableBalanceBefore() != null ? e.availableBalanceBefore().toString() : "0.00",
                        e.availableBalanceAfter() != null ? e.availableBalanceAfter().toString() : "0.00",
                        e.description() != null ? (e.description().length() > 18 ? e.description().substring(0, 15) + "..." : e.description()) : "-",
                        e.createdBy() != null ? e.createdBy() : "-"));
            }
            sb.append("========================================================================================================================\n");
            sb.append("                                                END OF STATEMENT                                                        \n");
            sb.append("========================================================================================================================\n");
            return sb.toString();
        }
    }

    public StatementResponse toResponse(StatementRequest entity) {
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

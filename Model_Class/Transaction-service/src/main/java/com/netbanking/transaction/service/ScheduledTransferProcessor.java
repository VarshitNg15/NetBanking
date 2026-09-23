package com.netbanking.transaction.service;

import com.netbanking.transaction.entity.Transaction;
import com.netbanking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferProcessor {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Scheduled(fixedDelay = 60000)
    public void processDueTransfers() {
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> dueTransfers = transactionRepository.findDueScheduledTransfers(now);
        if (!dueTransfers.isEmpty()) {
            log.info("Processing {} due scheduled transfers at {}", dueTransfers.size(), now);
            for (Transaction txn : dueTransfers) {
                try {
                    log.info("Executing scheduled transfer: {}", txn.getTransactionReference());
                    transactionService.executeImmediateTransfer(txn, null, null);
                } catch (Exception ex) {
                    log.error("Failed to execute scheduled transfer {}: {}", txn.getTransactionReference(), ex.getMessage(), ex);
                }
            }
        }
    }
}

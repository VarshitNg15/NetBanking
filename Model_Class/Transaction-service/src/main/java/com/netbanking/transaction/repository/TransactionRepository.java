package com.netbanking.transaction.repository;

import com.netbanking.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionReference(String transactionReference);

    List<Transaction> findByCustomerIdOrderByInitiatedAtDesc(String customerId);

    List<Transaction> findBySourceAccountIdOrDestinationAccountIdOrderByInitiatedAtDesc(Long sourceAccountId, Long destinationAccountId);

    @Query("SELECT t FROM Transaction t JOIN TransferDetails td ON td.transaction = t " +
           "WHERE td.transferMode = 'SCHEDULED' " +
           "AND t.transactionStatus = 'PROCESSING' " +
           "AND td.scheduledAt <= :dueTime")
    List<Transaction> findDueScheduledTransfers(@Param("dueTime") LocalDateTime dueTime);
}

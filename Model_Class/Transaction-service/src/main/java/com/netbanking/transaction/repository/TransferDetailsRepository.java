package com.netbanking.transaction.repository;

import com.netbanking.transaction.entity.TransferDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferDetailsRepository extends JpaRepository<TransferDetails, Long> {
    Optional<TransferDetails> findByTransactionTransactionId(Long transactionId);
}

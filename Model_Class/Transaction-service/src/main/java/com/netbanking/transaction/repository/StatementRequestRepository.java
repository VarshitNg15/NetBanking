package com.netbanking.transaction.repository;

import com.netbanking.transaction.entity.StatementRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementRequestRepository extends JpaRepository<StatementRequest, Long> {
    List<StatementRequest> findByCustomerIdOrderByRequestedAtDesc(String customerId);
}

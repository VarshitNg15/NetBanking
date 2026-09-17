package com.netbanking.user_service.repository;

import com.netbanking.user_service.entity.AccountOpeningRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountOpeningRequestRepository
        extends JpaRepository<AccountOpeningRequest, String> {

    List<AccountOpeningRequest> findByCustomerCustomerId(String customerId);

    List<AccountOpeningRequest> findByRequestStatus(String requestStatus);
}
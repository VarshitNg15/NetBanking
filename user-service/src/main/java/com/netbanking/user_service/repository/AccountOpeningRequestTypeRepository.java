package com.netbanking.user_service.repository;

import com.netbanking.user_service.entity.AccountOpeningRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountOpeningRequestTypeRepository
        extends JpaRepository<AccountOpeningRequestType, Long> {

    List<AccountOpeningRequestType> findByAccountOpeningRequestRequestId(
            String requestId
    );
}
package com.netbanking.user_service.repository;

import com.netbanking.user_service.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository
        extends JpaRepository<CustomerProfile, String> {
}
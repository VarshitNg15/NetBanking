package com.netbanking.user_service.repository;

import com.netbanking.user_service.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    boolean existsByCustomerId(String customerId);
}
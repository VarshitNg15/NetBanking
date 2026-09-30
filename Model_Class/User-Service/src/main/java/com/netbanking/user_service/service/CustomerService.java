package com.netbanking.user_service.service;

import com.netbanking.user_service.dto.CustomerRequest;
import com.netbanking.user_service.dto.CustomerResponse;
import com.netbanking.user_service.entity.Customer;
import com.netbanking.user_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final com.netbanking.user_service.metrics.UserMetrics userMetrics;

    public CustomerResponse createCustomer(CustomerRequest request) {

        if (customerRepository.existsByCustomerId(request.getCustomerId())) {

            throw new IllegalArgumentException(
                    "Customer already exists: " + request.getCustomerId()
            );
        }

        Customer customer = new Customer();

        customer.setCustomerId(request.getCustomerId());
        customer.setCustomerStatus("ACTIVE");

        Customer savedCustomer = customerRepository.save(customer);

        return toResponse(savedCustomer);
    }

    @Transactional
    public CustomerResponse getCustomerById(String customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found: " + customerId
                        )
                );

        if ("PENDING_APPROVAL".equalsIgnoreCase(customer.getCustomerStatus())) {
            customer.setCustomerStatus("ACTIVE");
            customer = customerRepository.save(customer);
        }

        userMetrics.recordGovernanceQuery("CUSTOMER_INSPECT");
        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        userMetrics.recordGovernanceQuery("ALL_CUSTOMERS");
        return customerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CustomerResponse updateCustomerStatus(
            String customerId,
            String status
    ) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    userMetrics.recordStatusChange(status, "FAILURE");
                    return new IllegalArgumentException(
                            "Customer not found: " + customerId
                    );
                });

        validateCustomerStatus(status);

        customer.setCustomerStatus(status);

        Customer updated = customerRepository.save(customer);
        userMetrics.recordStatusChange(status, "SUCCESS");
        return toResponse(updated);
    }

    /**
     * Used internally by other User Service components
     * when a JPA Customer entity is required.
     */
    @Transactional
    public Customer getCustomerEntityForInternalUse(String customerId) {
        return customerRepository.findById(customerId)
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setCustomerId(customerId);
                    customer.setCustomerStatus("ACTIVE");
                    return customerRepository.save(customer);
                });
    }

    private void validateCustomerStatus(String status) {

        if (status == null) {

            throw new IllegalArgumentException(
                    "Customer status cannot be null"
            );
        }

        switch (status) {

            case "PENDING_APPROVAL":
            case "ACTIVE":
            case "INACTIVE":
            case "BLOCKED":
            case "REJECTED":
                break;

            default:
                throw new IllegalArgumentException(
                        "Invalid customer status: " + status
                );
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        String name = customer.getCustomerId();
        if (customer.getCustomerProfile() != null) {
            String first = customer.getCustomerProfile().getFirstName() != null ? customer.getCustomerProfile().getFirstName().trim() : "";
            String last = customer.getCustomerProfile().getLastName() != null ? customer.getCustomerProfile().getLastName().trim() : "";
            String full = (first + " " + last).trim();
            if (!full.isEmpty()) {
                name = full;
            }
        }

        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .customerName(name)
                .customerStatus(customer.getCustomerStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
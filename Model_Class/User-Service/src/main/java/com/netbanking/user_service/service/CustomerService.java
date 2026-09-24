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

    public CustomerResponse createCustomer(CustomerRequest request) {

        if (customerRepository.existsByCustomerId(request.getCustomerId())) {

            throw new IllegalArgumentException(
                    "Customer already exists: " + request.getCustomerId()
            );
        }

        Customer customer = new Customer();

        customer.setCustomerId(request.getCustomerId());
        customer.setCustomerStatus("PENDING_APPROVAL");

        Customer savedCustomer = customerRepository.save(customer);

        return toResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(String customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found: " + customerId
                        )
                );

        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {

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
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found: " + customerId
                        )
                );

        validateCustomerStatus(status);

        customer.setCustomerStatus(status);

        return toResponse(customerRepository.save(customer));
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
                    customer.setCustomerStatus("PENDING_APPROVAL");
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

        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .customerStatus(customer.getCustomerStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
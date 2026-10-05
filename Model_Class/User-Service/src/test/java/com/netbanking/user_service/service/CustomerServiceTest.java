package com.netbanking.user_service.service;

import com.netbanking.user_service.dto.CustomerRequest;
import com.netbanking.user_service.dto.CustomerResponse;
import com.netbanking.user_service.entity.Customer;
import com.netbanking.user_service.entity.CustomerProfile;
import com.netbanking.user_service.metrics.UserMetrics;
import com.netbanking.user_service.repository.CustomerProfileRepository;
import com.netbanking.user_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private UserMetrics userMetrics;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;
    private CustomerProfile testProfile;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .customerId("CUST12345")
                .customerStatus("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testProfile = CustomerProfile.builder()
                .customerId("CUST12345")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    @DisplayName("createCustomer - Success")
    void testCreateCustomer_Success() {
        CustomerRequest request = new CustomerRequest("CUST12345");
        when(customerRepository.existsByCustomerId("CUST12345")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.createCustomer(request);

        assertNotNull(response);
        assertEquals("CUST12345", response.getCustomerId());
        assertEquals("ACTIVE", response.getCustomerStatus());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("createCustomer - Customer already exists throws IllegalArgumentException")
    void testCreateCustomer_AlreadyExists() {
        CustomerRequest request = new CustomerRequest("CUST12345");
        when(customerRepository.existsByCustomerId("CUST12345")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                customerService.createCustomer(request)
        );

        assertTrue(ex.getMessage().contains("Customer already exists"));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("getCustomerById - Found with profile")
    void testGetCustomerById_FoundWithProfile() {
        when(customerRepository.findById("CUST12345")).thenReturn(Optional.of(testCustomer));
        when(customerProfileRepository.findById("CUST12345")).thenReturn(Optional.of(testProfile));

        CustomerResponse response = customerService.getCustomerById("CUST12345");

        assertNotNull(response);
        assertEquals("CUST12345", response.getCustomerId());
        assertEquals("John Doe", response.getCustomerName());
        assertTrue(response.getKycCompleted());
        assertEquals("COMPLETED", response.getKycStatus());
        verify(userMetrics, times(1)).recordGovernanceQuery("CUSTOMER_INSPECT");
    }

    @Test
    @DisplayName("getCustomerById - PENDING_APPROVAL status auto-activates to ACTIVE")
    void testGetCustomerById_PendingApprovalAutoActivates() {
        testCustomer.setCustomerStatus("PENDING_APPROVAL");
        when(customerRepository.findById("CUST12345")).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.getCustomerById("CUST12345");

        assertEquals("ACTIVE", response.getCustomerStatus());
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    @DisplayName("getCustomerById - Not found throws IllegalArgumentException")
    void testGetCustomerById_NotFound() {
        when(customerRepository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                customerService.getCustomerById("NON_EXISTENT")
        );
    }

    @Test
    @DisplayName("getAllCustomers - Returns mapped response list")
    void testGetAllCustomers_Success() {
        when(customerRepository.findAll()).thenReturn(List.of(testCustomer));

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("CUST12345", responses.get(0).getCustomerId());
        verify(userMetrics, times(1)).recordGovernanceQuery("ALL_CUSTOMERS");
    }

    @Test
    @DisplayName("updateCustomerStatus - Valid status update success")
    void testUpdateCustomerStatus_Success() {
        when(customerRepository.findById("CUST12345")).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.updateCustomerStatus("CUST12345", "BLOCKED");

        assertNotNull(response);
        assertEquals("BLOCKED", response.getCustomerStatus());
        verify(userMetrics, times(1)).recordStatusChange("BLOCKED", "SUCCESS");
    }

    @Test
    @DisplayName("updateCustomerStatus - Customer not found throws exception and records metric")
    void testUpdateCustomerStatus_CustomerNotFound() {
        when(customerRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                customerService.updateCustomerStatus("UNKNOWN", "ACTIVE")
        );
        verify(userMetrics, times(1)).recordStatusChange("ACTIVE", "FAILURE");
    }

    @Test
    @DisplayName("updateCustomerStatus - Invalid status throws IllegalArgumentException")
    void testUpdateCustomerStatus_InvalidStatus() {
        when(customerRepository.findById("CUST12345")).thenReturn(Optional.of(testCustomer));

        assertThrows(IllegalArgumentException.class, () ->
                customerService.updateCustomerStatus("CUST12345", "INVALID_STATUS")
        );
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("updateCustomerStatus - Null status throws IllegalArgumentException")
    void testUpdateCustomerStatus_NullStatus() {
        when(customerRepository.findById("CUST12345")).thenReturn(Optional.of(testCustomer));

        assertThrows(IllegalArgumentException.class, () ->
                customerService.updateCustomerStatus("CUST12345", null)
        );
    }

    @Test
    @DisplayName("getCustomerEntityForInternalUse - Existing customer returns entity")
    void testGetCustomerEntityForInternalUse_Existing() {
        when(customerRepository.findById("CUST12345")).thenReturn(Optional.of(testCustomer));

        Customer result = customerService.getCustomerEntityForInternalUse("CUST12345");

        assertNotNull(result);
        assertEquals("CUST12345", result.getCustomerId());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("getCustomerEntityForInternalUse - Non-existing customer creates and returns entity")
    void testGetCustomerEntityForInternalUse_NotExistingCreatesNew() {
        when(customerRepository.findById("NEW123")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = customerService.getCustomerEntityForInternalUse("NEW123");

        assertNotNull(result);
        assertEquals("NEW123", result.getCustomerId());
        assertEquals("ACTIVE", result.getCustomerStatus());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Seed customer KYC status is pre-verified")
    void testSeedCustomer_KycStatus() {
        Customer seedCustomer = Customer.builder()
                .customerId("C5FCC99032132")
                .customerStatus("ACTIVE")
                .build();
        when(customerRepository.findById("C5FCC99032132")).thenReturn(Optional.of(seedCustomer));

        CustomerResponse response = customerService.getCustomerById("C5FCC99032132");

        assertTrue(response.getKycCompleted());
        assertEquals("COMPLETED", response.getKycStatus());
    }
}

package com.netbanking.user_service.service;

import com.netbanking.user_service.client.AccountServiceClient;
import com.netbanking.user_service.dto.AccountOpeningRequestDto;
import com.netbanking.user_service.dto.AccountOpeningResponseDto;
import com.netbanking.user_service.entity.AccountOpeningRequest;
import com.netbanking.user_service.entity.AccountOpeningRequestType;
import com.netbanking.user_service.entity.Customer;
import com.netbanking.user_service.entity.CustomerProfile;
import com.netbanking.user_service.metrics.UserMetrics;
import com.netbanking.user_service.repository.AccountOpeningRequestRepository;
import com.netbanking.user_service.repository.AccountOpeningRequestTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountOpeningServiceTest {

    @Mock
    private AccountOpeningRequestRepository requestRepository;

    @Mock
    private AccountOpeningRequestTypeRepository requestTypeRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserMetrics userMetrics;

    @InjectMocks
    private AccountOpeningService accountOpeningService;

    private Customer customer;
    private AccountOpeningRequest pendingRequest;

    @BeforeEach
    void setUp() {
        CustomerProfile profile = CustomerProfile.builder()
                .customerId("CUST100")
                .firstName("Robert")
                .lastName("Taylor")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        customer = Customer.builder()
                .customerId("CUST100")
                .customerStatus("ACTIVE")
                .customerProfile(profile)
                .build();

        pendingRequest = AccountOpeningRequest.builder()
                .requestId("REQ-001")
                .customer(customer)
                .requestStatus("PENDING")
                .submittedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createRequest - Success")
    void testCreateRequest_Success() {
        AccountOpeningRequestDto requestDto = AccountOpeningRequestDto.builder()
                .requestId("REQ-001")
                .customerId("CUST100")
                .accountTypes(List.of("SAVINGS", "CURRENT"))
                .build();

        when(customerService.getCustomerEntityForInternalUse("CUST100")).thenReturn(customer);
        when(requestRepository.existsById("REQ-001")).thenReturn(false);
        when(requestRepository.save(any(AccountOpeningRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountOpeningRequestType savings = new AccountOpeningRequestType(1L, pendingRequest, "SAVINGS");
        AccountOpeningRequestType current = new AccountOpeningRequestType(2L, pendingRequest, "CURRENT");
        when(requestTypeRepository.findByAccountOpeningRequestRequestId("REQ-001")).thenReturn(List.of(savings, current));

        AccountOpeningResponseDto response = accountOpeningService.createRequest(requestDto);

        assertNotNull(response);
        assertEquals("REQ-001", response.getRequestId());
        assertEquals("PENDING", response.getRequestStatus());
        assertEquals(2, response.getAccountTypes().size());
        verify(requestTypeRepository, times(2)).save(any(AccountOpeningRequestType.class));
        verify(userMetrics, times(1)).recordAccountOpening(eq("SUBMIT"), anyString(), eq("SUCCESS"));
    }

    @Test
    @DisplayName("createRequest - Already exists throws IllegalArgumentException")
    void testCreateRequest_AlreadyExists() {
        AccountOpeningRequestDto requestDto = AccountOpeningRequestDto.builder()
                .requestId("REQ-001")
                .customerId("CUST100")
                .accountTypes(List.of("SAVINGS"))
                .build();

        when(customerService.getCustomerEntityForInternalUse("CUST100")).thenReturn(customer);
        when(requestRepository.existsById("REQ-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                accountOpeningService.createRequest(requestDto)
        );
        verify(requestRepository, never()).save(any(AccountOpeningRequest.class));
    }

    @Test
    @DisplayName("createRequest - Invalid account type throws IllegalArgumentException")
    void testCreateRequest_InvalidAccountType() {
        AccountOpeningRequestDto requestDto = AccountOpeningRequestDto.builder()
                .requestId("REQ-002")
                .customerId("CUST100")
                .accountTypes(List.of("CRYPTO"))
                .build();

        when(customerService.getCustomerEntityForInternalUse("CUST100")).thenReturn(customer);
        when(requestRepository.existsById("REQ-002")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                accountOpeningService.createRequest(requestDto)
        );
        assertTrue(ex.getMessage().contains("Invalid account type"));
    }

    @Test
    @DisplayName("createRequest - Empty account types throws IllegalArgumentException")
    void testCreateRequest_EmptyAccountTypes() {
        AccountOpeningRequestDto requestDto = AccountOpeningRequestDto.builder()
                .requestId("REQ-003")
                .customerId("CUST100")
                .accountTypes(Collections.emptyList())
                .build();

        when(customerService.getCustomerEntityForInternalUse("CUST100")).thenReturn(customer);
        when(requestRepository.existsById("REQ-003")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                accountOpeningService.createRequest(requestDto)
        );
        assertTrue(ex.getMessage().contains("At least one account type is required"));
    }

    @Test
    @DisplayName("getRequest - Found returns response")
    void testGetRequest_Found() {
        when(requestRepository.findById("REQ-001")).thenReturn(Optional.of(pendingRequest));
        when(requestTypeRepository.findByAccountOpeningRequestRequestId("REQ-001"))
                .thenReturn(List.of(new AccountOpeningRequestType(1L, pendingRequest, "SAVINGS")));

        AccountOpeningResponseDto response = accountOpeningService.getRequest("REQ-001");

        assertNotNull(response);
        assertEquals("REQ-001", response.getRequestId());
        assertEquals("Robert Taylor", response.getCustomerName());
    }

    @Test
    @DisplayName("getRequest - Not found throws IllegalArgumentException")
    void testGetRequest_NotFound() {
        when(requestRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                accountOpeningService.getRequest("UNKNOWN")
        );
    }

    @Test
    @DisplayName("getCustomerRequests - Returns requests for customer")
    void testGetCustomerRequests() {
        when(requestRepository.findByCustomerCustomerId("CUST100")).thenReturn(List.of(pendingRequest));
        when(requestTypeRepository.findByAccountOpeningRequestRequestId("REQ-001")).thenReturn(Collections.emptyList());

        List<AccountOpeningResponseDto> result = accountOpeningService.getCustomerRequests("CUST100");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("REQ-001", result.get(0).getRequestId());
    }

    @Test
    @DisplayName("getPendingRequests - Returns pending requests and records metric")
    void testGetPendingRequests() {
        when(requestRepository.findByRequestStatus("PENDING")).thenReturn(List.of(pendingRequest));
        when(requestTypeRepository.findByAccountOpeningRequestRequestId("REQ-001")).thenReturn(Collections.emptyList());

        List<AccountOpeningResponseDto> result = accountOpeningService.getPendingRequests();

        assertEquals(1, result.size());
        verify(userMetrics, times(1)).recordGovernanceQuery("PENDING_ONBOARDING");
    }

    @Test
    @DisplayName("approveRequest - Success updates status and activates customer")
    void testApproveRequest_Success() {
        when(requestRepository.findById("REQ-001")).thenReturn(Optional.of(pendingRequest));
        when(requestRepository.save(any(AccountOpeningRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountOpeningResponseDto response = accountOpeningService.approveRequest("REQ-001", "ADMIN-42");

        assertNotNull(response);
        assertEquals("APPROVED", response.getRequestStatus());
        assertEquals("ADMIN-42", response.getReviewedBy());
        verify(customerService, times(1)).updateCustomerStatus("CUST100", "ACTIVE");
        verify(userMetrics, times(1)).recordAccountOpening("APPROVE", "ALL", "SUCCESS");
    }

    @Test
    @DisplayName("approveRequest - Not pending throws IllegalStateException")
    void testApproveRequest_NotPending_ThrowsException() {
        pendingRequest.setRequestStatus("APPROVED");
        when(requestRepository.findById("REQ-001")).thenReturn(Optional.of(pendingRequest));

        assertThrows(IllegalStateException.class, () ->
                accountOpeningService.approveRequest("REQ-001", "ADMIN-42")
        );
        verify(requestRepository, never()).save(any(AccountOpeningRequest.class));
    }

    @Test
    @DisplayName("rejectRequest - Success updates status and reason")
    void testRejectRequest_Success() {
        when(requestRepository.findById("REQ-001")).thenReturn(Optional.of(pendingRequest));
        when(requestRepository.save(any(AccountOpeningRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountOpeningResponseDto response = accountOpeningService.rejectRequest("REQ-001", "ADMIN-42", "Incomplete docs");

        assertNotNull(response);
        assertEquals("REJECTED", response.getRequestStatus());
        assertEquals("ADMIN-42", response.getReviewedBy());
        assertEquals("Incomplete docs", response.getRejectionReason());
        verify(userMetrics, times(1)).recordAccountOpening("REJECT", "ALL", "SUCCESS");
    }

    @Test
    @DisplayName("rejectRequest - Empty reason throws IllegalArgumentException")
    void testRejectRequest_EmptyReason_ThrowsException() {
        when(requestRepository.findById("REQ-001")).thenReturn(Optional.of(pendingRequest));

        assertThrows(IllegalArgumentException.class, () ->
                accountOpeningService.rejectRequest("REQ-001", "ADMIN-42", "   ")
        );
        verify(requestRepository, never()).save(any(AccountOpeningRequest.class));
    }
}

package com.netbanking.user_service.service;

import com.netbanking.user_service.dto.AccountApprovalRequest;
import com.netbanking.user_service.dto.AccountOpeningResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AccountOpeningService accountOpeningService;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("getPendingAccountRequests - Delegates to accountOpeningService")
    void testGetPendingAccountRequests() {
        AccountOpeningResponseDto dto = AccountOpeningResponseDto.builder()
                .requestId("REQ-1")
                .requestStatus("PENDING")
                .build();
        when(accountOpeningService.getPendingRequests()).thenReturn(List.of(dto));

        List<AccountOpeningResponseDto> results = adminService.getPendingAccountRequests();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("REQ-1", results.get(0).getRequestId());
        verify(accountOpeningService, times(1)).getPendingRequests();
    }

    @Test
    @DisplayName("approveAccountOpening - Delegates to accountOpeningService")
    void testApproveAccountOpening() {
        AccountApprovalRequest request = AccountApprovalRequest.builder()
                .adminId("ADMIN-1")
                .build();
        AccountOpeningResponseDto responseDto = AccountOpeningResponseDto.builder()
                .requestId("REQ-1")
                .requestStatus("APPROVED")
                .reviewedBy("ADMIN-1")
                .build();
        when(accountOpeningService.approveRequest("REQ-1", "ADMIN-1")).thenReturn(responseDto);

        AccountOpeningResponseDto result = adminService.approveAccountOpening("REQ-1", request);

        assertNotNull(result);
        assertEquals("APPROVED", result.getRequestStatus());
        assertEquals("ADMIN-1", result.getReviewedBy());
        verify(accountOpeningService, times(1)).approveRequest("REQ-1", "ADMIN-1");
    }

    @Test
    @DisplayName("rejectAccountOpening - Delegates to accountOpeningService")
    void testRejectAccountOpening() {
        AccountApprovalRequest request = AccountApprovalRequest.builder()
                .adminId("ADMIN-1")
                .reason("KYC document expired")
                .build();
        AccountOpeningResponseDto responseDto = AccountOpeningResponseDto.builder()
                .requestId("REQ-1")
                .requestStatus("REJECTED")
                .reviewedBy("ADMIN-1")
                .rejectionReason("KYC document expired")
                .build();
        when(accountOpeningService.rejectRequest("REQ-1", "ADMIN-1", "KYC document expired")).thenReturn(responseDto);

        AccountOpeningResponseDto result = adminService.rejectAccountOpening("REQ-1", request);

        assertNotNull(result);
        assertEquals("REJECTED", result.getRequestStatus());
        assertEquals("KYC document expired", result.getRejectionReason());
        verify(accountOpeningService, times(1)).rejectRequest("REQ-1", "ADMIN-1", "KYC document expired");
    }
}

package com.netbanking.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netbanking.user_service.dto.AccountApprovalRequest;
import com.netbanking.user_service.dto.AccountOpeningResponseDto;
import com.netbanking.user_service.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private AccountOpeningResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = AccountOpeningResponseDto.builder()
                .requestId("REQ-777")
                .customerId("CUST-777")
                .requestStatus("PENDING")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/users/admin/account-opening/pending - Returns 200 OK with pending list")
    void testGetPendingAccountOpeningRequests() throws Exception {
        when(adminService.getPendingAccountRequests()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/users/admin/account-opening/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("REQ-777"))
                .andExpect(jsonPath("$[0].requestStatus").value("PENDING"));

        verify(adminService, times(1)).getPendingAccountRequests();
    }

    @Test
    @DisplayName("POST /api/v1/users/admin/account-opening/{requestId}/approve - Returns 200 OK")
    void testApproveAccountOpening() throws Exception {
        AccountApprovalRequest approvalRequest = AccountApprovalRequest.builder()
                .adminId("ADMIN-01")
                .build();

        sampleResponse.setRequestStatus("APPROVED");
        sampleResponse.setReviewedBy("ADMIN-01");

        when(adminService.approveAccountOpening(eq("REQ-777"), any(AccountApprovalRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/users/admin/account-opening/REQ-777/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REQ-777"))
                .andExpect(jsonPath("$.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("ADMIN-01"));

        verify(adminService, times(1)).approveAccountOpening(eq("REQ-777"), any(AccountApprovalRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/users/admin/account-opening/{requestId}/reject - Returns 200 OK")
    void testRejectAccountOpening() throws Exception {
        AccountApprovalRequest rejectRequest = AccountApprovalRequest.builder()
                .adminId("ADMIN-01")
                .reason("Invalid address verification document")
                .build();

        sampleResponse.setRequestStatus("REJECTED");
        sampleResponse.setReviewedBy("ADMIN-01");
        sampleResponse.setRejectionReason("Invalid address verification document");

        when(adminService.rejectAccountOpening(eq("REQ-777"), any(AccountApprovalRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/users/admin/account-opening/REQ-777/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REQ-777"))
                .andExpect(jsonPath("$.requestStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Invalid address verification document"));

        verify(adminService, times(1)).rejectAccountOpening(eq("REQ-777"), any(AccountApprovalRequest.class));
    }
}

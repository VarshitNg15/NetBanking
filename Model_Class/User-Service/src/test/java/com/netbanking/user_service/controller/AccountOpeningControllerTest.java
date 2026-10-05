package com.netbanking.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netbanking.user_service.dto.AccountOpeningRequestDto;
import com.netbanking.user_service.dto.AccountOpeningResponseDto;
import com.netbanking.user_service.service.AccountOpeningService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountOpeningControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AccountOpeningService accountOpeningService;

    @InjectMocks
    private AccountOpeningController accountOpeningController;

    private AccountOpeningRequestDto sampleRequest;
    private AccountOpeningResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountOpeningController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleRequest = AccountOpeningRequestDto.builder()
                .requestId("REQ-500")
                .customerId("CUST-500")
                .accountTypes(List.of("SAVINGS"))
                .build();

        sampleResponse = AccountOpeningResponseDto.builder()
                .requestId("REQ-500")
                .customerId("CUST-500")
                .customerName("Jane Doe")
                .requestStatus("PENDING")
                .accountTypes(List.of("SAVINGS"))
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/users/account-opening - Returns 201 Created")
    void testCreateRequest() throws Exception {
        when(accountOpeningService.createRequest(any(AccountOpeningRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/users/account-opening")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value("REQ-500"))
                .andExpect(jsonPath("$.requestStatus").value("PENDING"));

        verify(accountOpeningService, times(1)).createRequest(any(AccountOpeningRequestDto.class));
    }

    @Test
    @DisplayName("GET /api/v1/users/account-opening/{requestId} - Returns 200 OK")
    void testGetRequest() throws Exception {
        when(accountOpeningService.getRequest("REQ-500")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/users/account-opening/REQ-500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("REQ-500"))
                .andExpect(jsonPath("$.customerName").value("Jane Doe"));

        verify(accountOpeningService, times(1)).getRequest("REQ-500");
    }

    @Test
    @DisplayName("GET /api/v1/users/account-opening/customer/{customerId} - Returns 200 OK with list")
    void testGetCustomerRequests() throws Exception {
        when(accountOpeningService.getCustomerRequests("CUST-500")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/users/account-opening/customer/CUST-500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("REQ-500"))
                .andExpect(jsonPath("$[0].customerId").value("CUST-500"));

        verify(accountOpeningService, times(1)).getCustomerRequests("CUST-500");
    }
}

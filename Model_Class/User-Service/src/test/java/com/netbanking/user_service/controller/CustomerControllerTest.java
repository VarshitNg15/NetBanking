package com.netbanking.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbanking.user_service.dto.CustomerRequest;
import com.netbanking.user_service.dto.CustomerResponse;
import com.netbanking.user_service.service.CustomerService;
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
class CustomerControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private CustomerResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController).build();

        sampleResponse = CustomerResponse.builder()
                .customerId("CUST-100")
                .customerName("John Doe")
                .customerStatus("ACTIVE")
                .kycCompleted(true)
                .kycStatus("COMPLETED")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/customers - Creates customer and returns 201 Created")
    void testCreateCustomer() throws Exception {
        CustomerRequest request = new CustomerRequest("CUST-100");
        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("CUST-100"))
                .andExpect(jsonPath("$.customerStatus").value("ACTIVE"));

        verify(customerService, times(1)).createCustomer(any(CustomerRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId} - Returns 200 OK")
    void testGetCustomer() throws Exception {
        when(customerService.getCustomerById("CUST-100")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/customers/CUST-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-100"))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.kycStatus").value("COMPLETED"));

        verify(customerService, times(1)).getCustomerById("CUST-100");
    }

    @Test
    @DisplayName("GET /api/v1/customers - Returns 200 OK with customer list")
    void testGetAllCustomers() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("CUST-100"));

        verify(customerService, times(1)).getAllCustomers();
    }

    @Test
    @DisplayName("PATCH /api/v1/customers/{customerId}/status - Returns 200 OK")
    void testUpdateCustomerStatus() throws Exception {
        sampleResponse.setCustomerStatus("BLOCKED");
        when(customerService.updateCustomerStatus(eq("CUST-100"), eq("BLOCKED"))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/customers/CUST-100/status")
                        .param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerStatus").value("BLOCKED"));

        verify(customerService, times(1)).updateCustomerStatus("CUST-100", "BLOCKED");
    }
}

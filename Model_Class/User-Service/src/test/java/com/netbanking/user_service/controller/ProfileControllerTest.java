package com.netbanking.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netbanking.user_service.dto.CustomerProfileRequest;
import com.netbanking.user_service.dto.CustomerProfileResponse;
import com.netbanking.user_service.service.CustomerProfileService;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CustomerProfileService customerProfileService;

    @InjectMocks
    private ProfileController profileController;

    private CustomerProfileRequest validRequest;
    private CustomerProfileResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validRequest = new CustomerProfileRequest();
        validRequest.setFirstName("Alice");
        validRequest.setLastName("Wonderland");
        validRequest.setDateOfBirth(LocalDate.of(1995, 6, 15));
        validRequest.setPhoneNumber("9876543210");
        validRequest.setAddressLine1("42 Wallaby Way");
        validRequest.setCity("Sydney");
        validRequest.setState("NSW");
        validRequest.setPostalCode("2000");
        validRequest.setCountry("Australia");

        sampleResponse = CustomerProfileResponse.builder()
                .customerId("CUST-100")
                .firstName("Alice")
                .lastName("Wonderland")
                .dateOfBirth(LocalDate.of(1995, 6, 15))
                .phoneNumber("9876543210")
                .addressLine1("42 Wallaby Way")
                .city("Sydney")
                .state("NSW")
                .postalCode("2000")
                .country("Australia")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/profile - Creates profile and returns 201 Created")
    void testCreateProfile() throws Exception {
        when(customerProfileService.createProfile(eq("CUST-100"), any(CustomerProfileRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/customers/CUST-100/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("CUST-100"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.city").value("Sydney"));

        verify(customerProfileService, times(1)).createProfile(eq("CUST-100"), any(CustomerProfileRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId}/profile - Returns 200 OK")
    void testGetProfile() throws Exception {
        when(customerProfileService.getProfile("CUST-100")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/customers/CUST-100/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-100"))
                .andExpect(jsonPath("$.firstName").value("Alice"));

        verify(customerProfileService, times(1)).getProfile("CUST-100");
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId}/kyc - Completed KYC returns 200 OK with COMPLETED")
    void testGetKycStatus_Completed() throws Exception {
        when(customerProfileService.isKycCompleted("CUST-100")).thenReturn(true);

        mockMvc.perform(get("/api/v1/customers/CUST-100/kyc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-100"))
                .andExpect(jsonPath("$.kycCompleted").value(true))
                .andExpect(jsonPath("$.kycStatus").value("COMPLETED"));

        verify(customerProfileService, times(1)).isKycCompleted("CUST-100");
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId}/kyc - Pending KYC returns 200 OK with PENDING")
    void testGetKycStatus_Pending() throws Exception {
        when(customerProfileService.isKycCompleted("CUST-100")).thenReturn(false);

        mockMvc.perform(get("/api/v1/customers/CUST-100/kyc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-100"))
                .andExpect(jsonPath("$.kycCompleted").value(false))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));

        verify(customerProfileService, times(1)).isKycCompleted("CUST-100");
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{customerId}/profile - Updates profile and returns 200 OK")
    void testUpdateProfile() throws Exception {
        sampleResponse.setLastName("Kingsleigh");
        when(customerProfileService.updateProfile(eq("CUST-100"), any(CustomerProfileRequest.class)))
                .thenReturn(sampleResponse);

        validRequest.setLastName("Kingsleigh");

        mockMvc.perform(put("/api/v1/customers/CUST-100/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Kingsleigh"));

        verify(customerProfileService, times(1)).updateProfile(eq("CUST-100"), any(CustomerProfileRequest.class));
    }
}

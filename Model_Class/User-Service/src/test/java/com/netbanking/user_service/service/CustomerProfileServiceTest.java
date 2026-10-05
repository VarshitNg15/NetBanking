package com.netbanking.user_service.service;

import com.netbanking.user_service.dto.CustomerProfileRequest;
import com.netbanking.user_service.dto.CustomerProfileResponse;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserMetrics userMetrics;

    @InjectMocks
    private CustomerProfileService customerProfileService;

    private CustomerProfileRequest validRequest;
    private CustomerProfile existingProfile;

    @BeforeEach
    void setUp() {
        validRequest = new CustomerProfileRequest();
        validRequest.setFirstName("Alice");
        validRequest.setLastName("Smith");
        validRequest.setDateOfBirth(LocalDate.of(1995, 5, 20));
        validRequest.setPhoneNumber("9876543210");
        validRequest.setAddressLine1("123 Main Street");
        validRequest.setAddressLine2("Apt 4B");
        validRequest.setCity("Mumbai");
        validRequest.setState("Maharashtra");
        validRequest.setPostalCode("400001");
        validRequest.setCountry("India");

        existingProfile = CustomerProfile.builder()
                .customerId("CUST999")
                .firstName("Alice")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .phoneNumber("9876543210")
                .addressLine1("123 Main Street")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .build();
    }

    @Test
    @DisplayName("createProfile - Success when new")
    void testCreateProfile_Success() {
        String customerId = "CUST999";
        when(customerService.getCustomerEntityForInternalUse(customerId)).thenReturn(new Customer());
        when(customerProfileRepository.existsById(customerId)).thenReturn(false);
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerProfileResponse response = customerProfileService.createProfile(customerId, validRequest);

        assertNotNull(response);
        assertEquals("Alice", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("9876543210", response.getPhoneNumber());
        verify(userMetrics, times(1)).recordProfileOperation("CREATE", "SUCCESS");
    }

    @Test
    @DisplayName("createProfile - Existing profile delegates to update")
    void testCreateProfile_AlreadyExists_Updates() {
        String customerId = "CUST999";
        when(customerService.getCustomerEntityForInternalUse(customerId)).thenReturn(new Customer());
        when(customerProfileRepository.existsById(customerId)).thenReturn(true);
        when(customerProfileRepository.findById(customerId)).thenReturn(Optional.of(existingProfile));
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        validRequest.setLastName("Johnson");
        CustomerProfileResponse response = customerProfileService.createProfile(customerId, validRequest);

        assertNotNull(response);
        assertEquals("Johnson", response.getLastName());
        verify(userMetrics, times(1)).recordProfileOperation("UPDATE", "SUCCESS");
    }

    @Test
    @DisplayName("createProfile - Under 18 years old throws IllegalArgumentException")
    void testCreateProfile_Under18_ThrowsException() {
        String customerId = "CUST999";
        when(customerService.getCustomerEntityForInternalUse(customerId)).thenReturn(new Customer());
        when(customerProfileRepository.existsById(customerId)).thenReturn(false);

        validRequest.setDateOfBirth(LocalDate.now().minusYears(17));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                customerProfileService.createProfile(customerId, validRequest)
        );
        assertTrue(ex.getMessage().contains("Minimum age to open account is 18 years"));
    }

    @Test
    @DisplayName("createProfile - DOB before 1925 throws IllegalArgumentException")
    void testCreateProfile_DobBefore1925_ThrowsException() {
        String customerId = "CUST999";
        when(customerService.getCustomerEntityForInternalUse(customerId)).thenReturn(new Customer());
        when(customerProfileRepository.existsById(customerId)).thenReturn(false);

        validRequest.setDateOfBirth(LocalDate.of(1920, 1, 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                customerProfileService.createProfile(customerId, validRequest)
        );
        assertTrue(ex.getMessage().contains("Date of birth should start from 01-01-1925"));
    }

    @Test
    @DisplayName("createProfile - Invalid phone number digits throws IllegalArgumentException")
    void testCreateProfile_InvalidPhone_ThrowsException() {
        String customerId = "CUST999";
        when(customerService.getCustomerEntityForInternalUse(customerId)).thenReturn(new Customer());
        when(customerProfileRepository.existsById(customerId)).thenReturn(false);

        validRequest.setPhoneNumber("12345"); // invalid length

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                customerProfileService.createProfile(customerId, validRequest)
        );
        assertTrue(ex.getMessage().contains("Mobile number must be exactly 10 digits"));
    }

    @Test
    @DisplayName("createProfile - Missing first name throws IllegalArgumentException")
    void testCreateProfile_MissingFirstName_ThrowsException() {
        String customerId = "CUST999";
        when(customerService.getCustomerEntityForInternalUse(customerId)).thenReturn(new Customer());
        when(customerProfileRepository.existsById(customerId)).thenReturn(false);

        validRequest.setFirstName("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                customerProfileService.createProfile(customerId, validRequest)
        );
        assertTrue(ex.getMessage().contains("First name is required"));
    }

    @Test
    @DisplayName("getProfile - Found returns profile response")
    void testGetProfile_Found() {
        String customerId = "CUST999";
        when(customerProfileRepository.findById(customerId)).thenReturn(Optional.of(existingProfile));

        CustomerProfileResponse response = customerProfileService.getProfile(customerId);

        assertNotNull(response);
        assertEquals("Alice", response.getFirstName());
        verify(userMetrics, times(1)).recordProfileOperation("GET", "SUCCESS");
    }

    @Test
    @DisplayName("getProfile - Not found throws exception and records metric")
    void testGetProfile_NotFound() {
        String customerId = "UNKNOWN";
        when(customerProfileRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                customerProfileService.getProfile(customerId)
        );
        verify(userMetrics, times(1)).recordProfileOperation("GET", "NOT_FOUND");
    }

    @Test
    @DisplayName("isKycCompleted - Seed customer returns true")
    void testIsKycCompleted_SeedCustomer() {
        assertTrue(customerProfileService.isKycCompleted("C5FCC99032132"));
        assertTrue(customerProfileService.isKycCompleted("C0106071918AA"));
        assertTrue(customerProfileService.isKycCompleted("CFA32EB91C801"));
    }

    @Test
    @DisplayName("isKycCompleted - Regular customer with valid profile returns true")
    void testIsKycCompleted_RegularCustomer_WithProfile() {
        when(customerProfileRepository.findById("REGULAR123")).thenReturn(Optional.of(existingProfile));

        assertTrue(customerProfileService.isKycCompleted("REGULAR123"));
    }

    @Test
    @DisplayName("isKycCompleted - Regular customer without profile returns false")
    void testIsKycCompleted_RegularCustomer_NoProfile() {
        when(customerProfileRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertFalse(customerProfileService.isKycCompleted("UNKNOWN"));
    }

    @Test
    @DisplayName("isKycCompleted - Null or empty customerId returns false")
    void testIsKycCompleted_NullOrEmpty() {
        assertFalse(customerProfileService.isKycCompleted(null));
        assertFalse(customerProfileService.isKycCompleted("   "));
    }

    @Test
    @DisplayName("updateProfile - Profile not found throws exception")
    void testUpdateProfile_NotFound() {
        when(customerProfileRepository.findById("NOT_FOUND")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                customerProfileService.updateProfile("NOT_FOUND", validRequest)
        );
        verify(userMetrics, times(1)).recordProfileOperation("UPDATE", "NOT_FOUND");
    }
}

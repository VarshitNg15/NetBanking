package com.netbanking.user_service.service;

import com.netbanking.user_service.dto.CustomerProfileRequest;
import com.netbanking.user_service.dto.CustomerProfileResponse;
import com.netbanking.user_service.entity.CustomerProfile;
import com.netbanking.user_service.repository.CustomerProfileRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerService customerService;
    private final com.netbanking.user_service.metrics.UserMetrics userMetrics;

    // ============================================================
    // CREATE PROFILE
    // ============================================================

    public CustomerProfileResponse createProfile(
            String customerId,
            CustomerProfileRequest request
    ) {

        // Verify that the customer exists.
        customerService.getCustomerEntityForInternalUse(customerId);

        // If profile already exists, gracefully update it
        if (customerProfileRepository.existsById(customerId)) {
            return updateProfile(customerId, request);
        }

        CustomerProfile profile = new CustomerProfile();

        // CUSTOMER_ID is both the primary key and foreign key
        // in CUSTOMER_PROFILE.
        profile.setCustomerId(customerId);

        copyRequestToEntity(request, profile);

        CustomerProfile savedProfile =
                customerProfileRepository.save(profile);

        userMetrics.recordProfileOperation("CREATE", "SUCCESS");
        return toResponse(savedProfile);
    }


    // ============================================================
    // GET PROFILE
    // ============================================================

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(
            String customerId
    ) {

        CustomerProfile profile =
                customerProfileRepository.findById(customerId)
                        .orElseThrow(() -> {
                            userMetrics.recordProfileOperation("GET", "NOT_FOUND");
                            return new IllegalArgumentException(
                                    "Profile not found for customer: "
                                            + customerId
                            );
                        });

        userMetrics.recordProfileOperation("GET", "SUCCESS");
        return toResponse(profile);
    }


    // ============================================================
    // UPDATE PROFILE
    // ============================================================

    public CustomerProfileResponse updateProfile(
            String customerId,
            CustomerProfileRequest request
    ) {

        CustomerProfile existingProfile =
                customerProfileRepository.findById(customerId)
                        .orElseThrow(() -> {
                            userMetrics.recordProfileOperation("UPDATE", "NOT_FOUND");
                            return new IllegalArgumentException(
                                    "Profile not found for customer: "
                                            + customerId
                            );
                        });

        copyRequestToEntity(request, existingProfile);

        CustomerProfile updatedProfile =
                customerProfileRepository.save(existingProfile);

        userMetrics.recordProfileOperation("UPDATE", "SUCCESS");
        return toResponse(updatedProfile);
    }


    // ============================================================
    // COPY DTO -> ENTITY
    // ============================================================

    private void copyRequestToEntity(
            CustomerProfileRequest request,
            CustomerProfile profile
    ) {

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        profile.setFirstName(request.getFirstName().trim());

        profile.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);

        if (request.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        java.time.LocalDate minDob = java.time.LocalDate.of(1925, 1, 1);
        if (request.getDateOfBirth().isBefore(minDob)) {
            throw new IllegalArgumentException("Date of birth should start from 01-01-1925, not before that");
        }
        java.time.LocalDate maxDob = java.time.LocalDate.now().minusYears(18);
        if (request.getDateOfBirth().isAfter(maxDob)) {
            throw new IllegalArgumentException("Minimum age to open account is 18 years from current date");
        }
        profile.setDateOfBirth(request.getDateOfBirth());

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            String cleanPhone = request.getPhoneNumber().replaceAll("\\D", "");
            if (cleanPhone.length() != 10) {
                throw new IllegalArgumentException("Mobile number must be exactly 10 digits");
            }
            profile.setPhoneNumber(cleanPhone);
        } else {
            profile.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAddressLine1() == null || request.getAddressLine1().trim().isEmpty()) {
            throw new IllegalArgumentException("Address Line 1 is required");
        }
        profile.setAddressLine1(request.getAddressLine1().trim());

        profile.setAddressLine2(request.getAddressLine2() != null ? request.getAddressLine2().trim() : null);

        if (request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        profile.setCity(request.getCity().trim());

        if (request.getState() == null || request.getState().trim().isEmpty()) {
            throw new IllegalArgumentException("State is required");
        }
        profile.setState(request.getState().trim());

        if (request.getPostalCode() == null || request.getPostalCode().trim().isEmpty()) {
            throw new IllegalArgumentException("PIN code is required");
        }
        profile.setPostalCode(request.getPostalCode().trim());

        if (request.getCountry() == null || request.getCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Country is required");
        }
        profile.setCountry(request.getCountry().trim());
    }


    // ============================================================
    // ENTITY -> DTO
    // ============================================================

    private CustomerProfileResponse toResponse(
            CustomerProfile profile
    ) {

        return CustomerProfileResponse.builder()

                .customerId(profile.getCustomerId())

                .firstName(profile.getFirstName())

                .lastName(profile.getLastName())

                .dateOfBirth(profile.getDateOfBirth())

                .phoneNumber(profile.getPhoneNumber())

                .addressLine1(profile.getAddressLine1())

                .addressLine2(profile.getAddressLine2())

                .city(profile.getCity())

                .state(profile.getState())

                .postalCode(profile.getPostalCode())

                .country(profile.getCountry())

                .createdAt(profile.getCreatedAt())

                .updatedAt(profile.getUpdatedAt())

                .build();
    }
}
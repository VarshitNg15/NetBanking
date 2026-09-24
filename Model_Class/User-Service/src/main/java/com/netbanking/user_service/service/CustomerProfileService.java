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
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Profile not found for customer: "
                                                + customerId
                                )
                        );

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
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Profile not found for customer: "
                                                + customerId
                                )
                        );

        copyRequestToEntity(request, existingProfile);

        CustomerProfile updatedProfile =
                customerProfileRepository.save(existingProfile);

        return toResponse(updatedProfile);
    }


    // ============================================================
    // COPY DTO -> ENTITY
    // ============================================================

    private void copyRequestToEntity(
            CustomerProfileRequest request,
            CustomerProfile profile
    ) {

        profile.setFirstName(request.getFirstName());

        profile.setLastName(request.getLastName());

        profile.setDateOfBirth(request.getDateOfBirth());

        profile.setPhoneNumber(request.getPhoneNumber());

        profile.setAddressLine1(request.getAddressLine1());

        profile.setAddressLine2(request.getAddressLine2());

        profile.setCity(request.getCity());

        profile.setState(request.getState());

        profile.setPostalCode(request.getPostalCode());

        profile.setCountry(request.getCountry());
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
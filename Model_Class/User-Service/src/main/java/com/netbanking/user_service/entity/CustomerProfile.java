package com.netbanking.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER_PROFILE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {

    @Id
    @Column(name = "CUSTOMER_ID", length = 50, nullable = false)
    private String customerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "CUSTOMER_ID",
            referencedColumnName = "CUSTOMER_ID",
            insertable = false,
            updatable = false
    )
    private Customer customer;

    @Column(name = "FIRST_NAME", length = 100, nullable = false)
    private String firstName;

    @Column(name = "LAST_NAME", length = 100)
    private String lastName;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    @Column(name = "PHONE_NUMBER", length = 20)
    private String phoneNumber;

    @Column(name = "ADDRESS_LINE1", length = 255)
    private String addressLine1;

    @Column(name = "ADDRESS_LINE2", length = 255)
    private String addressLine2;

    @Column(name = "CITY", length = 100)
    private String city;

    @Column(name = "STATE", length = 100)
    private String state;

    @Column(name = "POSTAL_CODE", length = 20)
    private String postalCode;

    @Column(name = "COUNTRY", length = 100)
    private String country;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
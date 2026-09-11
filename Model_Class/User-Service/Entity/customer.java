package com.netbanking.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CUSTOMER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "CUSTOMER_ID", length = 50)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "CUSTOMER_STATUS", nullable = false, length = 30)
    private CustomerStatus customerStatus;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    /*
     * One customer has exactly one profile.
     */
    @OneToOne(
        mappedBy = "customer",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private CustomerProfile profile;

    /*
     * One customer can submit multiple account opening requests.
     */
    @OneToMany(
        mappedBy = "customer",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<AccountOpeningRequest> accountOpeningRequests =
            new ArrayList<>();
}
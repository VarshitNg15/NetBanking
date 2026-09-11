package com.netbanking.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "ACCOUNT_OPENING_REQUEST_TYPE",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_REQUEST_ACCOUNT_TYPE",
            columnNames = {"REQUEST_ID", "ACCOUNT_TYPE"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOpeningRequestType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REQUEST_TYPE_ID")
    private Long requestTypeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "REQUEST_ID",
        nullable = false,
        foreignKey =
            @ForeignKey(name = "FK_REQUEST_TYPE_REQUEST")
    )
    private AccountOpeningRequest accountOpeningRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCOUNT_TYPE", nullable = false, length = 20)
    private AccountType accountType;
}
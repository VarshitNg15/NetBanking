package com.netbanking.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "ACCOUNT_OPENING_REQUEST_TYPE",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_REQUEST_ACCOUNT_TYPE",
                        columnNames = {
                                "REQUEST_ID",
                                "ACCOUNT_TYPE"
                        }
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
    @Column(name = "REQUEST_TYPE_ID", nullable = false)
    private Long requestTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "REQUEST_ID",
            nullable = false
    )
    private AccountOpeningRequest accountOpeningRequest;

    @Column(name = "ACCOUNT_TYPE", length = 20, nullable = false)
    private String accountType;
}
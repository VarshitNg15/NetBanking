package com.netbanking.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT_PIN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountPin {

    @Id
    @Column(name = "ACCOUNT_ID")
    private Long accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "ACCOUNT_ID")
    private Account account;

    @Column(name = "PIN_HASH", nullable = false, length = 255)
    private String pinHash;

    @Column(name = "PIN_CREATED_AT", nullable = false)
    private LocalDateTime pinCreatedAt;

    @Column(name = "FAILED_ATTEMPTS", nullable = false)
    private Integer failedAttempts;

    @Column(name = "LOCKED_UNTIL")
    private LocalDateTime lockedUntil;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}
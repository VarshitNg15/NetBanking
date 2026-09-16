package com.netbanking.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOGIN_HISTORY")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOGIN_HISTORY_ID")
    private Long loginHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private AuthUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOGIN_STATUS", nullable = false, length = 30)
    private LoginStatus loginStatus;

    @Column(name = "IP_ADDRESS", length = 45)
    private String ipAddress;

    @Column(name = "USER_AGENT", length = 500)
    private String userAgent;

    @Column(name = "LOGIN_AT", nullable = false)
    private LocalDateTime loginAt;

    @PrePersist
    void prePersist() { loginAt = LocalDateTime.now(); }
}

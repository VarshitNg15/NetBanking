@Entity
@Table(name = "AUTH_USER")
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "CUSTOMER_ID", unique = true)
    private String customerId;

    @Column(name = "EMAIL", nullable = false, unique = true)
    private String email;

    @Column(name = "PASSWORD_HASH", nullable = false)
    private String passwordHash;

    @Column(name = "ACCOUNT_STATUS", nullable = false)
    private String accountStatus;

    @Column(name = "EMAIL_VERIFIED", nullable = false)
    private Character emailVerified;

    @Column(name = "MFA_ENABLED", nullable = false)
    private Character mfaEnabled;

    @Column(name = "FAILED_LOGIN_ATTEMPTS", nullable = false)
    private Integer failedLoginAttempts;

    @Column(name = "LOCKED_UNTIL")
    private LocalDateTime lockedUntil;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    // Getters and setters
}
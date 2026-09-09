@Entity
@Table(name = "PASSWORD_RESET_TOKEN")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESET_TOKEN_ID")
    private Long resetTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private AuthUser user;

    @Column(name = "TOKEN_HASH", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // Getters and setters
}
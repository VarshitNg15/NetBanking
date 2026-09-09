@Entity
@Table(name = "REFRESH_TOKEN")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REFRESH_TOKEN_ID")
    private Long refreshTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private AuthUser user;

    @Column(name = "TOKEN_HASH", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "REVOKED_AT")
    private LocalDateTime revokedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // Getters and setters
}
@Entity
@Table(name = "EMAIL_OTP")
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OTP_ID")
    private Long otpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private AuthUser user;

    @Column(name = "OTP_HASH", nullable = false)
    private String otpHash;

    @Column(name = "OTP_PURPOSE", nullable = false)
    private String otpPurpose;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "VERIFIED_AT")
    private LocalDateTime verifiedAt;

    @Column(name = "ATTEMPT_COUNT", nullable = false)
    private Integer attemptCount;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // Getters and setters
}
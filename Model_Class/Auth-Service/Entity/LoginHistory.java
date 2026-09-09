@Entity
@Table(name = "LOGIN_HISTORY")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOGIN_HISTORY_ID")
    private Long loginHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private AuthUser user;

    @Column(name = "LOGIN_STATUS", nullable = false)
    private String loginStatus;

    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @Column(name = "USER_AGENT")
    private String userAgent;

    @Column(name = "LOGIN_AT", nullable = false)
    private LocalDateTime loginAt;

    // Getters and setters
}
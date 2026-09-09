@Entity
@Table(
    name = "USER_ROLE",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "ROLE_NAME"})
    }
)
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ROLE_ID")
    private Long userRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private AuthUser user;

    @Column(name = "ROLE_NAME", nullable = false)
    private String roleName;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    // Getters and setters
}
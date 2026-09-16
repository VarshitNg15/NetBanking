package com.netbanking.auth.repository;

import com.netbanking.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findByEmailIgnoreCase(String email);
    Optional<AuthUser> findByCustomerId(String customerId);
    boolean existsByEmailIgnoreCase(String email);
}

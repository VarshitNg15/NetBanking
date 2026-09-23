package com.netbanking.auth.service;

import com.netbanking.auth.entity.AccountStatus;
import com.netbanking.auth.entity.AuthUser;
import com.netbanking.auth.entity.RoleName;
import com.netbanking.auth.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        com.netbanking.auth.config.DotenvLoader.load();
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty("JWT_SECRET");
        }
        if (secret == null || secret.isBlank()) {
            secret = io.jsonwebtoken.io.Encoders.BASE64.encode(io.jsonwebtoken.Jwts.SIG.HS256.key().build().getEncoded());
        }
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 900000L); // 15 min
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days
        ReflectionTestUtils.setField(jwtService, "issuer", "netbanking-auth-service");
    }

    @Test
    void testGenerateAndValidateToken() {
        AuthUser user = AuthUser.builder()
                .customerId("C100200300")
                .email("testuser@netbanking.com")
                .accountStatus(AccountStatus.ACTIVE)
                .roles(new ArrayList<>())
                .build();
        user.getRoles().add(UserRole.builder().user(user).roleName(RoleName.CUSTOMER).build());

        JwtService.TokenData tokenData = jwtService.generateAccessToken(user);

        assertNotNull(tokenData);
        assertNotNull(tokenData.token());
        assertEquals(900, tokenData.expiresIn());
        assertEquals(List.of("CUSTOMER"), tokenData.roles());

        // Validation
        assertTrue(jwtService.validateToken(tokenData.token()));
        assertFalse(jwtService.isTokenExpired(tokenData.token()));

        // Claim extractions
        assertEquals("C100200300", jwtService.extractCustomerId(tokenData.token()));
        assertEquals("testuser@netbanking.com", jwtService.extractEmail(tokenData.token()));
        assertEquals(List.of("CUSTOMER"), jwtService.extractRoles(tokenData.token()));
        assertTrue(jwtService.getRemainingExpirationSeconds(tokenData.token()) > 0);
    }

    @Test
    void testGeneratePasswordResetToken() {
        AuthUser user = AuthUser.builder()
                .customerId("C100200300")
                .email("testuser@netbanking.com")
                .accountStatus(AccountStatus.ACTIVE)
                .roles(new ArrayList<>())
                .build();

        JwtService.TokenData tokenData = jwtService.generatePasswordResetToken(user);

        assertNotNull(tokenData);
        assertNotNull(tokenData.token());
        assertEquals(900, tokenData.expiresIn());
        assertEquals(List.of("ROLE_RESET_PASSWORD"), tokenData.roles());

        assertTrue(jwtService.validateToken(tokenData.token()));
        assertEquals("PASSWORD_RESET", jwtService.extractPurpose(tokenData.token()));
        assertEquals("C100200300", jwtService.extractCustomerId(tokenData.token()));
        assertEquals("testuser@netbanking.com", jwtService.extractEmail(tokenData.token()));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.here"));
        assertNull(jwtService.extractPurpose("invalid.token.here"));
    }
}

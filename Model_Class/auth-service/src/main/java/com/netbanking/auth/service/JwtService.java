package com.netbanking.auth.service;

import com.netbanking.auth.entity.AuthUser;
import com.netbanking.auth.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.issuer:http://localhost:8081}")
    private String issuer;

    @Value("${app.jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    public TokenData generateAccessToken(AuthUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);
        List<String> roles = user.getRoles().stream().map(UserRole::getRoleName).map(Enum::name).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(user.getCustomerId())
                .claim("email", user.getEmail())
                .claim("customerId", user.getCustomerId())
                .claim("roles", roles)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new TokenData(token, expiry.getEpochSecond() - now.getEpochSecond(), roles);
    }

    public record TokenData(String token, long expiresIn, List<String> roles) {}
}

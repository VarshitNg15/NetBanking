package com.netbanking.auth.service;

import com.netbanking.auth.entity.AuthUser;
import com.netbanking.auth.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:900000}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Value("${jwt.issuer:netbanking-auth-service}")
    private String issuer;

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < 32) {
                keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenData generateAccessToken(AuthUser user) {
        long now = System.currentTimeMillis();
        long expiry = now + jwtExpiration;
        List<String> roles = user.getRoles().stream()
                .map(UserRole::getRoleName)
                .map(Enum::name)
                .toList();

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(user.getCustomerId())
                .claim("customerId", user.getCustomerId())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();

        return new TokenData(token, jwtExpiration / 1000, roles);
    }

    public TokenData generatePasswordResetToken(AuthUser user) {
        long now = System.currentTimeMillis();
        long expiry = now + (15 * 60 * 1000); // 15 minutes
        List<String> roles = List.of("ROLE_RESET_PASSWORD");

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(user.getCustomerId())
                .claim("customerId", user.getCustomerId())
                .claim("email", user.getEmail())
                .claim("purpose", "PASSWORD_RESET")
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();

        return new TokenData(token, 900L, roles);
    }

    public String extractPurpose(String token) {
        try {
            return extractClaim(token, claims -> claims.get("purpose", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    public String extractCustomerId(String token) {
        Claims claims = extractAllClaims(token);
        String customerId = claims.get("customerId", String.class);
        return customerId != null ? customerId : claims.getSubject();
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles", List.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public long getRemainingExpirationSeconds(String token) {
        try {
            Date exp = extractExpiration(token);
            long diff = exp.getTime() - System.currentTimeMillis();
            return Math.max(0, diff / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    public record TokenData(String token, long expiresIn, List<String> roles) {}
}

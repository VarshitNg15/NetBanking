package com.netbanking.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/verify-otp",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/v3/api-docs",
            "/swagger-ui",
            "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Skip authentication if endpoint is whitelisted
        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            return chain.filter(exchange);
        }

        // 2. Validate Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // 3. Parse and validate pure JWT
        try {
            Claims claims = extractClaims(token);
            String purpose = claims.get("purpose", String.class);
            if ("PASSWORD_RESET".equalsIgnoreCase(purpose)) {
                log.warn("Blocked attempt to use password-reset token on protected endpoint: {}", path);
                return onError(exchange, HttpStatus.FORBIDDEN, "This temporary access token can only be used to reset password");
            }

            String customerId = claims.get("customerId", String.class);
            if (customerId == null || customerId.isBlank()) {
                customerId = claims.getSubject();
            }
            String email = claims.get("email", String.class);

            Object rolesObj = claims.get("roles");
            String roles = "";
            if (rolesObj instanceof List<?> roleList) {
                roles = String.join(",", roleList.stream().map(Object::toString).toList());
            }

            // 4. Propagate trusted edge headers to downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Customer-Id", customerId != null ? customerId : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Roles", roles)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException | IllegalArgumentException ex) {
            log.error("JWT validation error for request {}: {}", path, ex.getMessage());
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
            if (keyBytes.length < 32) {
                keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = "{\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message + "\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

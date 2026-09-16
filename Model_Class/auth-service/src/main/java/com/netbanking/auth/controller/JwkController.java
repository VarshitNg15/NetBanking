package com.netbanking.auth.controller;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwkController {
    private final RSAKey rsaKey;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new com.nimbusds.jose.jwk.JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> oauthJwks() {
        return jwks();
    }

    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> openidConfiguration() {
        String issuer = "http://localhost:8081";
        return Map.of(
                "issuer", issuer,
                "jwks_uri", issuer + "/.well-known/jwks.json",
                "id_token_signing_alg_values_supported", new String[]{"RS256"},
                "response_types_supported", new String[]{"token"},
                "subject_types_supported", new String[]{"public"}
        );
    }
}

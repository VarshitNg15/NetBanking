package com.netbanking.auth.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfig {

    @Bean
    public RSAKey rsaKey(
            @Value("${app.jwt.key-id:netbanking-auth-key}") String keyId,
            @Value("${app.jwt.private-key:classpath:keys/private.pem}") String privateKeyLocation,
            @Value("${app.jwt.public-key:classpath:keys/public.pem}") String publicKeyLocation) {
        try {
            byte[] privateBytes = readPem(privateKeyLocation, "PRIVATE KEY");
            byte[] publicBytes = readPem(publicKeyLocation, "PUBLIC KEY");

            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(publicBytes));

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load RSA JWT signing keys", ex);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    private byte[] readPem(String location, String type) throws Exception {
        String content;
        if (location.startsWith("classpath:")) {
            var resource = getClass().getResourceAsStream("/" + location.substring("classpath:".length()));
            if (resource == null) {
                throw new IllegalStateException("JWT key resource not found: " + location);
            }
            content = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        } else {
            content = Files.readString(Path.of(location));
        }
        String base64 = content
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}

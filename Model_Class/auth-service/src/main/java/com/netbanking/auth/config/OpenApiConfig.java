package com.netbanking.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NetBanking Authentication Service API",
                version = "1.0.0",
                description = "Authentication, authorization, OTP/MFA verification, and JWT issuance service for NetBanking.",
                contact = @Contact(name = "NetBanking Platform Engineering", email = "support@netbanking.com")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "API Gateway URL"),
                @Server(url = "http://localhost:8081", description = "Direct Auth Service URL")
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Bearer Token authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}

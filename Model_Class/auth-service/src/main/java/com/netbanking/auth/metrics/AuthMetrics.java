package com.netbanking.auth.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AuthMetrics {

    private final MeterRegistry registry;

    public AuthMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordLogin(String role, String status, String reason) {
        Counter.builder("netbanking.auth.login.requests")
                .description("Total user and admin login attempts initiated from the frontend")
                .tag("role", role != null ? role : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .tag("reason", reason != null ? reason : "NONE")
                .register(registry)
                .increment();
    }

    public void recordLoginDuration(long durationNanos) {
        Timer.builder("netbanking.auth.login.duration")
                .description("Duration of login authentication process")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordOtpIssued(String purpose) {
        Counter.builder("netbanking.auth.otp.issued")
                .description("Total MFA and verification OTPs issued")
                .tag("purpose", purpose != null ? purpose : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordOtpVerified(String purpose, String status) {
        Counter.builder("netbanking.auth.otp.verified")
                .description("Total OTP verification attempts performed from frontend")
                .tag("purpose", purpose != null ? purpose : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordRegistration(String status) {
        Counter.builder("netbanking.auth.registration.requests")
                .description("Customer registration requests from frontend")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordTokenRefresh(String status) {
        Counter.builder("netbanking.auth.token.refresh")
                .description("JWT token refresh operations triggered by frontend sessions")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordLogout() {
        Counter.builder("netbanking.auth.logout.requests")
                .description("User and admin sign-out actions triggered from frontend")
                .register(registry)
                .increment();
    }

    public void recordPasswordReset(String step, String status) {
        Counter.builder("netbanking.auth.password.reset")
                .description("Password recovery and reset operations from frontend")
                .tag("step", step != null ? step : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }
}

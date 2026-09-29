package com.netbanking.user_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class UserMetrics {

    private final MeterRegistry registry;

    public UserMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordProfileOperation(String action, String status) {
        Counter.builder("netbanking.customer.profile.requests")
                .description("Customer KYC profile operations performed from frontend")
                .tag("action", action != null ? action : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordProfileDuration(String action, long durationNanos) {
        Timer.builder("netbanking.customer.profile.duration")
                .description("Duration of customer profile operations")
                .tag("action", action != null ? action : "UNKNOWN")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordStatusChange(String targetStatus, String status) {
        Counter.builder("netbanking.customer.status.changes")
                .description("Admin changes to customer account statuses")
                .tag("target_status", targetStatus != null ? targetStatus : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordAccountOpening(String action, String accountType, String status) {
        Counter.builder("netbanking.customer.account_opening.requests")
                .description("Customer account opening applications and admin approvals/rejections")
                .tag("action", action != null ? action : "UNKNOWN")
                .tag("account_type", accountType != null ? accountType : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordGovernanceQuery(String scope) {
        Counter.builder("netbanking.customer.governance.queries")
                .description("Admin customer directory and inspection queries from frontend")
                .tag("scope", scope != null ? scope : "ALL")
                .register(registry)
                .increment();
    }
}

package com.netbanking.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class NotificationMetrics {

    private final MeterRegistry registry;

    public NotificationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordFetch(String scope) {
        Counter.builder("netbanking.notification.fetch.requests")
                .description("Notification center fetch requests from frontend")
                .tag("scope", scope != null ? scope : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordEmailDispatched(String type, String status) {
        Counter.builder("netbanking.notification.email.dispatched")
                .description("Customer email notifications dispatched via Google SMTP")
                .tag("type", type != null ? type : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordEmailDuration(long durationNanos) {
        Timer.builder("netbanking.notification.email.duration")
                .description("Duration of email delivery via SMTP")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordAuditAccess(String action, String status) {
        Counter.builder("netbanking.audit.access.requests")
                .description("Audit trail access passkey requests and approvals")
                .tag("action", action != null ? action : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }
}

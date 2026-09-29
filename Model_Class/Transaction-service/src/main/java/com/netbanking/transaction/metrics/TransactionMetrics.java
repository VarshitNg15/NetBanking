package com.netbanking.transaction.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class TransactionMetrics {

    private final MeterRegistry registry;

    public TransactionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordTransfer(String type, BigDecimal amount, String status) {
        Counter.builder("netbanking.transfer.requests")
                .description("Fund transfer operations (Internal, External, Self) executed from frontend")
                .tag("type", type != null ? type : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();

        if (amount != null && "SUCCESS".equalsIgnoreCase(status)) {
            DistributionSummary.builder("netbanking.transfer.amount")
                    .description("Value of money transferred across accounts in INR")
                    .baseUnit("INR")
                    .register(registry)
                    .record(amount.doubleValue());
        }
    }

    public void recordTransferDuration(long durationNanos) {
        Timer.builder("netbanking.transfer.duration")
                .description("Latency of end-to-end fund transfer execution")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordHistoryQuery(String scope, String status) {
        Counter.builder("netbanking.transaction.history.queries")
                .description("Transaction history lookups from customer portal and admin ledger")
                .tag("scope", scope != null ? scope : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordStatementAction(String action, String status) {
        Counter.builder("netbanking.statement.requests")
                .description("Account statement generation, inquiry, and download operations")
                .tag("action", action != null ? action : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordStatementDownloadDuration(long durationNanos) {
        Timer.builder("netbanking.statement.download.duration")
                .description("Time taken to generate and download statement content")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }
}

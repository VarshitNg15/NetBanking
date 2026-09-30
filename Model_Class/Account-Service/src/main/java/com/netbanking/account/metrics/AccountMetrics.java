package com.netbanking.account.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountMetrics {

    private final MeterRegistry registry;

    public AccountMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAccountCreation(String type, String status) {
        Counter.builder("netbanking.account.create.requests")
                .description("New account creation requests executed from frontend")
                .tag("type", type != null ? type : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordAccountFetch(String queryType) {
        Counter.builder("netbanking.account.fetch.requests")
                .description("Account retrieval and list queries from frontend")
                .tag("query", queryType != null ? queryType : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordBalanceCheck(String status) {
        Counter.builder("netbanking.account.balance.checks")
                .description("Account balance inquiries from dashboard and transfers")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordDeposit(BigDecimal amount, String status) {
        Counter.builder("netbanking.account.deposit.requests")
                .description("Cash deposit operations from frontend")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();

        if (amount != null && "SUCCESS".equalsIgnoreCase(status)) {
            DistributionSummary.builder("netbanking.account.deposit.amount")
                    .description("Value of deposits processed in INR")
                    .baseUnit("INR")
                    .register(registry)
                    .record(amount.doubleValue());
        }
    }

    public void recordDebit(BigDecimal amount, String status) {
        Counter.builder("netbanking.account.debit.requests")
                .description("Account debit postings")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordStatusChange(String newStatus, String status) {
        Counter.builder("netbanking.account.status.changes")
                .description("Admin account state governance actions (Active, Frozen, Blocked, Closed)")
                .tag("new_status", newStatus != null ? newStatus : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordPinOperation(String action, String status) {
        Counter.builder("netbanking.account.pin.operations")
                .description("4-digit account security PIN setup, update, and verification operations")
                .tag("action", action != null ? action : "UNKNOWN")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }

    public void recordLedgerQuery(String status) {
        Counter.builder("netbanking.account.ledger.queries")
                .description("Live account ledger inquiries from customer and admin dashboards")
                .tag("status", status != null ? status : "UNKNOWN")
                .register(registry)
                .increment();
    }
}

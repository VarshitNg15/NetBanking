package com.netbanking.account.controller;

import com.netbanking.account.dto.request.BalanceOperationRequest;
import com.netbanking.account.dto.request.ChangeStatusRequest;
import com.netbanking.account.dto.request.ChangeTypeRequest;
import com.netbanking.account.dto.request.CreateAccountRequest;
import com.netbanking.account.dto.request.PostingRequest;
import com.netbanking.account.dto.request.SetPinRequest;
import com.netbanking.account.dto.response.AccountResponse;
import com.netbanking.account.dto.response.BalanceOperationResponse;
import com.netbanking.account.dto.response.BalanceResponse;
import com.netbanking.account.dto.response.LedgerResponse;
import com.netbanking.account.entity.EntryType;
import com.netbanking.account.service.AccountService;
import com.netbanking.account.service.BalanceService;
import com.netbanking.account.service.LedgerService;
import com.netbanking.account.service.PinService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/accounts", "/api/accounts"})
@Tag(name = "Account & Ledger Operations", description = "Endpoints for bank account creation, balances, debit/credit postings, and 4-digit PIN")
public class AccountController {

    private final AccountService accounts;
    private final BalanceService balances;
    private final LedgerService ledger;
    private final PinService pins;
    private final com.netbanking.account.metrics.AccountMetrics metrics;

    public AccountController(AccountService a, BalanceService b, LedgerService l, PinService p,
                             com.netbanking.account.metrics.AccountMetrics m) {
        accounts = a;
        balances = b;
        ledger = l;
        pins = p;
        metrics = m;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest r) {
        String typeStr = r.accountType() != null ? r.accountType().name() : "UNKNOWN";
        try {
            AccountResponse response = accounts.create(r);
            metrics.recordAccountCreation(typeStr, "SUCCESS");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            metrics.recordAccountCreation(typeStr, "FAILURE");
            throw e;
        }
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        metrics.recordAccountFetch("SINGLE");
        return accounts.get(id);
    }

    @GetMapping({"/number/{accountNumber}", "/by-number/{accountNumber}"})
    public AccountResponse getByAccountNumber(@PathVariable String accountNumber) {
        return accounts.getByAccountNumber(accountNumber);
    }

    @GetMapping
    public List<AccountResponse> byCustomer(@RequestParam(required = false) String customerId) {
        if (customerId == null || customerId.isBlank()) {
            metrics.recordAccountFetch("ALL");
            return accounts.getAll();
        }
        metrics.recordAccountFetch("BY_CUSTOMER");
        return accounts.byCustomer(customerId);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse status(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest r) {
        String newStatus = r.status() != null ? r.status().name() : "UNKNOWN";
        try {
            AccountResponse resp = accounts.changeStatus(id, r);
            metrics.recordStatusChange(newStatus, "SUCCESS");
            return resp;
        } catch (Exception e) {
            metrics.recordStatusChange(newStatus, "FAILURE");
            throw e;
        }
    }

    @PatchMapping("/{id}/type")
    public AccountResponse type(@PathVariable Long id, @Valid @RequestBody ChangeTypeRequest r) {
        return accounts.changeType(id, r);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable Long id) {
        try {
            BalanceResponse resp = balances.get(id);
            metrics.recordBalanceCheck("SUCCESS");
            return resp;
        } catch (Exception e) {
            metrics.recordBalanceCheck("FAILURE");
            throw e;
        }
    }

    @PostMapping("/{id}/credits")
    public LedgerResponse credit(@PathVariable Long id, @Valid @RequestBody PostingRequest r) {
        try {
            LedgerResponse resp = balances.post(id, EntryType.CREDIT, r);
            metrics.recordDeposit(r.amount(), "SUCCESS");
            return resp;
        } catch (Exception e) {
            metrics.recordDeposit(r.amount(), "FAILURE");
            throw e;
        }
    }

    @PostMapping("/{id}/debits")
    public LedgerResponse debit(@PathVariable Long id, @Valid @RequestBody PostingRequest r) {
        try {
            LedgerResponse resp = balances.post(id, EntryType.DEBIT, r);
            metrics.recordDebit(r.amount(), "SUCCESS");
            return resp;
        } catch (Exception e) {
            metrics.recordDebit(r.amount(), "FAILURE");
            throw e;
        }
    }

    @PostMapping("/{id}/debit")
    public BalanceOperationResponse debitOperation(@PathVariable Long id, @Valid @RequestBody BalanceOperationRequest r) {
        try {
            PostingRequest posting = new PostingRequest(
                    r.transactionReference(),
                    r.transactionReference() + "-DEBIT",
                    r.amount(),
                    r.description(),
                    r.initiatedBy() != null ? r.initiatedBy() : "SYSTEM"
            );
            balances.post(id, EntryType.DEBIT, posting);
            metrics.recordDebit(r.amount(), "SUCCESS");
            return new BalanceOperationResponse(true, "Account debited successfully");
        } catch (Exception e) {
            metrics.recordDebit(r.amount(), "FAILURE");
            throw e;
        }
    }

    @PostMapping("/{id}/credit")
    public BalanceOperationResponse creditOperation(@PathVariable Long id, @Valid @RequestBody BalanceOperationRequest r) {
        try {
            PostingRequest posting = new PostingRequest(
                    r.transactionReference(),
                    r.transactionReference() + "-CREDIT",
                    r.amount(),
                    r.description(),
                    r.initiatedBy() != null ? r.initiatedBy() : "SYSTEM"
            );
            balances.post(id, EntryType.CREDIT, posting);
            metrics.recordDeposit(r.amount(), "SUCCESS");
            return new BalanceOperationResponse(true, "Account credited successfully");
        } catch (Exception e) {
            metrics.recordDeposit(r.amount(), "FAILURE");
            throw e;
        }
    }

    @GetMapping("/{id}/ledger")
    public List<LedgerResponse> ledger(@PathVariable Long id) {
        try {
            List<LedgerResponse> resp = ledger.list(id);
            metrics.recordLedgerQuery("SUCCESS");
            return resp;
        } catch (Exception e) {
            metrics.recordLedgerQuery("FAILURE");
            throw e;
        }
    }

    @RequestMapping(value = "/{id}/pin", method = {RequestMethod.PUT, RequestMethod.POST})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPin(@PathVariable Long id, @Valid @RequestBody SetPinRequest r,
                       @RequestHeader(value = "X-Customer-Email", required = false) String customerEmail,
                       @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        String email = (customerEmail != null && !customerEmail.isBlank()) ? customerEmail : userEmail;
        try {
            pins.set(id, r.pin(), email);
            metrics.recordPinOperation("SET", "SUCCESS");
        } catch (Exception e) {
            metrics.recordPinOperation("SET", "FAILURE");
            throw e;
        }
    }

    @GetMapping("/{id}/pin/status")
    public Map<String, Object> pinStatus(@PathVariable Long id) {
        return Map.of("accountId", id, "pinSet", pins.isPinSet(id));
    }

    @PostMapping("/{id}/pin/verify")
    public Map<String, Boolean> verifyPin(@PathVariable Long id, @Valid @RequestBody SetPinRequest r) {
        boolean valid = pins.verify(id, r.pin());
        metrics.recordPinOperation("VERIFY", valid ? "SUCCESS" : "INVALID");
        return Map.of("valid", valid);
    }
}

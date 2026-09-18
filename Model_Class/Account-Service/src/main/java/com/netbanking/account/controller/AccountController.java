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

    public AccountController(AccountService a, BalanceService b, LedgerService l, PinService p) {
        accounts = a;
        balances = b;
        ledger = l;
        pins = p;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.create(r));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accounts.get(id);
    }

    @GetMapping
    public List<AccountResponse> byCustomer(@RequestParam String customerId) {
        return accounts.byCustomer(customerId);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse status(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest r) {
        return accounts.changeStatus(id, r);
    }

    @PatchMapping("/{id}/type")
    public AccountResponse type(@PathVariable Long id, @Valid @RequestBody ChangeTypeRequest r) {
        return accounts.changeType(id, r);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable Long id) {
        return balances.get(id);
    }

    @PostMapping("/{id}/credits")
    public LedgerResponse credit(@PathVariable Long id, @Valid @RequestBody PostingRequest r) {
        return balances.post(id, EntryType.CREDIT, r);
    }

    @PostMapping("/{id}/debits")
    public LedgerResponse debit(@PathVariable Long id, @Valid @RequestBody PostingRequest r) {
        return balances.post(id, EntryType.DEBIT, r);
    }

    @PostMapping("/{id}/debit")
    public BalanceOperationResponse debitOperation(@PathVariable Long id, @Valid @RequestBody BalanceOperationRequest r) {
        PostingRequest posting = new PostingRequest(
                r.transactionReference(),
                r.transactionReference() + "-DEBIT",
                r.amount(),
                r.description(),
                r.initiatedBy() != null ? r.initiatedBy() : "SYSTEM"
        );
        balances.post(id, EntryType.DEBIT, posting);
        return new BalanceOperationResponse(true, "Account debited successfully");
    }

    @PostMapping("/{id}/credit")
    public BalanceOperationResponse creditOperation(@PathVariable Long id, @Valid @RequestBody BalanceOperationRequest r) {
        PostingRequest posting = new PostingRequest(
                r.transactionReference(),
                r.transactionReference() + "-CREDIT",
                r.amount(),
                r.description(),
                r.initiatedBy() != null ? r.initiatedBy() : "SYSTEM"
        );
        balances.post(id, EntryType.CREDIT, posting);
        return new BalanceOperationResponse(true, "Account credited successfully");
    }

    @GetMapping("/{id}/ledger")
    public List<LedgerResponse> ledger(@PathVariable Long id) {
        return ledger.list(id);
    }

    @PutMapping("/{id}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPin(@PathVariable Long id, @Valid @RequestBody SetPinRequest r) {
        pins.set(id, r.pin());
    }

    @PostMapping("/{id}/pin/verify")
    public Map<String, Boolean> verifyPin(@PathVariable Long id, @Valid @RequestBody SetPinRequest r) {
        return Map.of("valid", pins.verify(id, r.pin()));
    }
}

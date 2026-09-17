package com.netbanking.account.controller;
import com.netbanking.account.dto.request.*; 
import com.netbanking.account.dto.response.*;
import com.netbanking.account.entity.EntryType; 
import com.netbanking.account.service.*; 
import jakarta.validation.Valid; 
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*; 
import java.util.*;
@RestController
@RequestMapping("/api/v1/accounts") 
public class AccountController {
	private final AccountService accounts;
	private final BalanceService balances;
	private final LedgerService ledger;
	private final PinService pins;
	public AccountController(AccountService a,BalanceService b,LedgerService l,PinService p){
		accounts=a;
		balances=b;
		ledger=l;
		pins=p;
		}
 @PostMapping 
 public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest r){
	 return ResponseEntity.status(HttpStatus.CREATED).body(accounts.create(r));
	 }
 @GetMapping("/{id}")
 public AccountResponse get(@PathVariable Long id){
	 return accounts.get(id);
	 }
 @GetMapping
 public List<AccountResponse> byCustomer(@RequestParam String customerId){
	 return accounts.byCustomer(customerId);
	 }
 @PatchMapping("/{id}/status")
 public AccountResponse status(@PathVariable Long id,@Valid @RequestBody ChangeStatusRequest r){
	 return accounts.changeStatus(id,r);
	 }
 @PatchMapping("/{id}/type") 
 public AccountResponse type(@PathVariable Long id,@Valid @RequestBody ChangeTypeRequest r){
	 return accounts.changeType(id,r);
	 }
 @GetMapping("/{id}/balance")
 public BalanceResponse balance(@PathVariable Long id){
	 return balances.get(id);
	 }
 @PostMapping("/{id}/credits")
 public LedgerResponse
 credit(@PathVariable Long id,@Valid @RequestBody PostingRequest r){
	 return balances.post(id,EntryType.CREDIT,r);
	 }
 @PostMapping("/{id}/debits")
 public LedgerResponse debit(@PathVariable Long id,@Valid @RequestBody PostingRequest r){
	 return balances.post(id,EntryType.DEBIT,r);
	 }
 @GetMapping("/{id}/ledger")
 public List<LedgerResponse> ledger(@PathVariable Long id){
	 return ledger.list(id);
	 }
 @PutMapping("/{id}/pin") @ResponseStatus(HttpStatus.NO_CONTENT) 
 public void setPin(@PathVariable Long id,@Valid @RequestBody SetPinRequest r){pins.set(id,r.pin());
 }
 @PostMapping("/{id}/pin/verify")
 public Map<String,Boolean> verifyPin(@PathVariable Long id,@Valid @RequestBody SetPinRequest r){
	 return Map.of("valid",pins.verify(id,r.pin()));
	 }}

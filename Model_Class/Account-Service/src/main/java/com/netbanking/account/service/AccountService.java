package com.netbanking.account.service;
import com.netbanking.account.dto.request.*; 
import com.netbanking.account.dto.response.AccountResponse;
import com.netbanking.account.entity.*;
import com.netbanking.account.exception.*;
import com.netbanking.account.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service 
public class AccountService {
 private final AccountRepository accounts; 
 private final AccountBalanceRepository balances; 
 private final AccountTypeHistoryRepository history;
 public AccountService(AccountRepository a,AccountBalanceRepository b,AccountTypeHistoryRepository h){
	 accounts=a;
	 balances=b;
	 history=h;
	 }
 @Transactional 
 public AccountResponse create(CreateAccountRequest request){
     List<Account> existing = accounts.findByCustomerId(request.customerId());
     boolean alreadyHasType = existing.stream().anyMatch(acc -> 
         acc.getAccountType() == request.accountType() && acc.getAccountStatus() != AccountStatus.CLOSED
     );
     if (alreadyHasType) {
         throw new ConflictException("Customer %s already has an active or pending %s account. A customer may only have one SAVINGS and one CURRENT account.".formatted(request.customerId(), request.accountType()));
     }
     long nonClosedCount = existing.stream().filter(acc -> acc.getAccountStatus() != AccountStatus.CLOSED).count();
     if (nonClosedCount >= 2) {
         throw new ConflictException("Customer %s has reached the maximum allowed limit of accounts (1 SAVINGS and 1 CURRENT account).".formatted(request.customerId()));
     }

	 Account a=new Account();
	 a.setCustomerId(request.customerId());
	 a.setAccountNumber(nextAccountNumber());
	 a.setAccountType(request.accountType());
	 a.setCurrencyCode(request.currencyCode()==null?"INR":request.currencyCode());
	 accounts.save(a);
	 balances.save(new AccountBalance(a));
	 return AccountResponse.from(a);
 }
 @Transactional(readOnly=true)
 public AccountResponse get(Long id){
	 return AccountResponse.from(account(id));
 }
 @Transactional(readOnly=true)
 public List<AccountResponse> getAll(){
	 return accounts.findAll().stream().map(AccountResponse::from).toList();
 }
 @Transactional(readOnly=true)
 public List<AccountResponse> byCustomer(String customerId){
	 return accounts.findByCustomerId(customerId).stream().map(AccountResponse::from).toList();
 }
 @Transactional
 public AccountResponse changeStatus(Long id,ChangeStatusRequest r){
	 Account a=account(id); if(a.getAccountStatus()==AccountStatus.CLOSED) throw new ConflictException("A closed account cannot change status");
	 if(r.status()==AccountStatus.CLOSED && (r.closureReason()==null||r.closureReason().isBlank())) throw new IllegalArgumentException("closureReason is required to close an account");
	 a.setAccountStatus(r.status()); 
	 if(r.status()==AccountStatus.ACTIVE&&a.getApprovedAt()==null)a.setApprovedAt(LocalDateTime.now());
	 if(r.status()==AccountStatus.CLOSED){a.setClosedAt(LocalDateTime.now());a.setClosureReason(r.closureReason());
	 }
	 return AccountResponse.from(a);
	 }
 @Transactional 
 public AccountResponse changeType(Long id,ChangeTypeRequest r){
	 Account a=account(id);
	 if(a.getAccountType()==r.accountType()) throw new ConflictException("Account already has this type");
	 history.save(new AccountTypeHistory(a,a.getAccountType(),r.accountType(),r.changedBy(),r.reason()));a.setAccountType(r.accountType());
	 return AccountResponse.from(a);}
 Account account(Long id){
	 return accounts.findById(id).orElseThrow(()->new NotFoundException("Account %d was not found".formatted(id)));}
 private String nextAccountNumber(){String v;do{v="NB"+(100000000000000000L+Math.abs(UUID.randomUUID().getMostSignificantBits())%900000000000000000L);}
 while(accounts.findByAccountNumber(v).isPresent());
 return v;
 }
}

package com.netbanking.account.service;
import com.netbanking.account.dto.request.PostingRequest; 
import com.netbanking.account.dto.response.*; 
import com.netbanking.account.entity.*; 
import com.netbanking.account.exception.*;
import com.netbanking.account.repository.*; 
import org.springframework.stereotype.Service; 
import org.springframework.transaction.annotation.Transactional;
@Service
public class BalanceService { 
	private final AccountService accounts; 
	private final AccountBalanceRepository balances; 
	private final AccountLedgerRepository ledger;
 public BalanceService(AccountService a,AccountBalanceRepository b,AccountLedgerRepository l){
	 accounts=a;balances=b;ledger=l;
	 }
 @Transactional(readOnly=true) 
 public BalanceResponse get(Long id){accounts.account(id);
 return BalanceResponse.from(balances.findById(id).orElseThrow());}
 @Transactional
 public LedgerResponse post(Long id,EntryType type,PostingRequest r){
	 Account a=accounts.account(id);
	 if(a.getAccountStatus()!=AccountStatus.ACTIVE)throw new ConflictException("Only active accounts may be posted");if(ledger.existsByEntryReference(r.entryReference()))throw new ConflictException("entryReference has already been processed");AccountBalance b=balances.findByIdForUpdate(id).orElseThrow();var before=b.getCurrentBalance();var availableBefore=b.getAvailableBalance();if(type==EntryType.CREDIT)b.credit(r.amount());else b.debit(r.amount());AccountLedger entry=ledger.save(new AccountLedger(a,r.transactionReference(),r.entryReference(),type,r.amount(),before,b.getCurrentBalance(),availableBefore,b.getAvailableBalance(),r.description(),r.createdBy()));return LedgerResponse.from(entry);}}

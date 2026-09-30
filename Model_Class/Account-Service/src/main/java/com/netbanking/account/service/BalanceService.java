package com.netbanking.account.service;
import com.netbanking.account.client.UserServiceClient;
import com.netbanking.account.dto.request.PostingRequest; 
import com.netbanking.account.dto.response.*; 
import com.netbanking.account.entity.*; 
import com.netbanking.account.exception.*;
import com.netbanking.account.repository.*; 
import java.math.BigDecimal;
import org.springframework.stereotype.Service; 
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceService { 
	private static final BigDecimal MAX_TRANSACTION_LIMIT = new BigDecimal("10000000.00");
	private final AccountService accounts; 
	private final AccountBalanceRepository balances; 
	private final AccountLedgerRepository ledger;
	private final UserServiceClient userServiceClient;

	public BalanceService(AccountService a, AccountBalanceRepository b, AccountLedgerRepository l, UserServiceClient u) {
		accounts = a;
		balances = b;
		ledger = l;
		userServiceClient = u;
	}

	@Transactional(readOnly=true) 
	public BalanceResponse get(Long id) {
		accounts.account(id);
		return BalanceResponse.from(balances.findById(id).orElseThrow());
	}

	@Transactional
	public LedgerResponse post(Long id, EntryType type, PostingRequest r) {
		if (r.amount() != null && r.amount().compareTo(MAX_TRANSACTION_LIMIT) > 0) {
			throw new IllegalArgumentException("Deposit/Transaction amount exceeds maximum allowed limit of ₹1,00,00,000 (1 Crore INR)");
		}
		Account a = accounts.account(id);
		if (a.getAccountStatus() != AccountStatus.ACTIVE) {
			throw new ConflictException("Only active accounts may be posted");
		}

		// Enforce mandatory KYC (customer profile setup) for deposits/credits
		if (type == EntryType.CREDIT) {
			String customerId = a.getCustomerId();
			boolean kycOk = userServiceClient.isKycCompleted(customerId);
			if (!kycOk) {
				throw new ConflictException("KYC Verification Required: Customer (" + customerId + ") has not completed profile info setup. Direct deposits are prohibited until KYC profile setup is completed.");
			}
		}

		if (ledger.existsByEntryReference(r.entryReference())) {
			throw new ConflictException("entryReference has already been processed");
		}
		AccountBalance b = balances.findByIdForUpdate(id).orElseThrow();
		var before = b.getCurrentBalance();
		var availableBefore = b.getAvailableBalance();
		if (type == EntryType.CREDIT) {
			b.credit(r.amount());
		} else {
			b.debit(r.amount());
		}
		AccountLedger entry = ledger.save(new AccountLedger(a, r.transactionReference(), r.entryReference(), type, r.amount(), before, b.getCurrentBalance(), availableBefore, b.getAvailableBalance(), r.description(), r.createdBy()));
		return LedgerResponse.from(entry);
	}
}

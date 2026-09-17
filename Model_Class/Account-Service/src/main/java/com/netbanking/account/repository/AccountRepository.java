package com.netbanking.account.repository;
import com.netbanking.account.entity.Account; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountRepository extends JpaRepository<Account,Long>{ Optional<Account> findByAccountNumber(String accountNumber); List<Account> findByCustomerId(String customerId); }

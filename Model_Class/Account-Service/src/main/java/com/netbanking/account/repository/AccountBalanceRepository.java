package com.netbanking.account.repository;
import com.netbanking.account.entity.AccountBalance;
import org.springframework.data.jpa.repository.*;
import java.util.*;


import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountBalanceRepository extends JpaRepository<AccountBalance,Long>{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from AccountBalance b where b.accountId=:id") Optional<AccountBalance> findByIdForUpdate(Long id); }

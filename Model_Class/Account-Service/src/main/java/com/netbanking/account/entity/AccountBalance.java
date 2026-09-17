package com.netbanking.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="ACCOUNT_BALANCE")
public class AccountBalance {
  @Id @Column(name="ACCOUNT_ID") private Long accountId;
  @OneToOne @MapsId @JoinColumn(name="ACCOUNT_ID") private Account account;
  @Column(name="CURRENT_BALANCE", nullable=false) private BigDecimal currentBalance = BigDecimal.ZERO;
  @Column(name="AVAILABLE_BALANCE", nullable=false) private BigDecimal availableBalance = BigDecimal.ZERO;
  @Column(name="TOTAL_CREDIT", nullable=false) private BigDecimal totalCredit = BigDecimal.ZERO;
  @Column(name="TOTAL_DEBIT", nullable=false) private BigDecimal totalDebit = BigDecimal.ZERO;
  @Version @Column(name="VERSION_NO", nullable=false) private Long versionNo;
  protected AccountBalance(){} public AccountBalance(Account account){this.account=account;}
  public BigDecimal getCurrentBalance(){return currentBalance;} public BigDecimal getAvailableBalance(){return availableBalance;}
  public void credit(BigDecimal amount){currentBalance=currentBalance.add(amount);availableBalance=availableBalance.add(amount);totalCredit=totalCredit.add(amount);}
  public void debit(BigDecimal amount){if(availableBalance.compareTo(amount)<0) throw new IllegalStateException("Insufficient available balance"); currentBalance=currentBalance.subtract(amount);availableBalance=availableBalance.subtract(amount);totalDebit=totalDebit.add(amount);}
}

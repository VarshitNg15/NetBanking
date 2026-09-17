package com.netbanking.account.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "ACCOUNT")
public class Account {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ACCOUNT_ID") private Long id;
  @Column(name = "CUSTOMER_ID", nullable = false, length = 50) private String customerId;
  @Column(name = "ACCOUNT_NUMBER", nullable = false, unique = true, length = 20) private String accountNumber;
  @Enumerated(EnumType.STRING) @Column(name = "ACCOUNT_TYPE", nullable = false) private AccountType accountType;
  @Enumerated(EnumType.STRING) @Column(name = "ACCOUNT_STATUS", nullable = false) private AccountStatus accountStatus = AccountStatus.PENDING_APPROVAL;
  @Column(name = "CURRENCY_CODE", nullable = false, length = 3) private String currencyCode = "INR";
  @Column(name = "CREATED_AT", nullable = false, updatable = false) private LocalDateTime createdAt;
  @Column(name = "UPDATED_AT", nullable = false) private LocalDateTime updatedAt;
  @Column(name = "APPROVED_AT") private LocalDateTime approvedAt;
  @Column(name = "CLOSED_AT") private LocalDateTime closedAt;
  @Column(name = "CLOSURE_REASON", length = 500) private String closureReason;
  @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); }
  @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
  public Long getId(){return id;} public String getCustomerId(){return customerId;} public String getAccountNumber(){return accountNumber;} public AccountType getAccountType(){return accountType;} public AccountStatus getAccountStatus(){return accountStatus;} public String getCurrencyCode(){return currencyCode;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getApprovedAt(){return approvedAt;}
  public void setCustomerId(String v){customerId=v;} public void setAccountNumber(String v){accountNumber=v;} public void setAccountType(AccountType v){accountType=v;} public void setAccountStatus(AccountStatus v){accountStatus=v;} public void setCurrencyCode(String v){currencyCode=v;} public void setApprovedAt(LocalDateTime v){approvedAt=v;} public void setClosedAt(LocalDateTime v){closedAt=v;} public void setClosureReason(String v){closureReason=v;}
}

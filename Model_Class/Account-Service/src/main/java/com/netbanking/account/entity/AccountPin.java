package com.netbanking.account.entity;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="ACCOUNT_PIN") public class AccountPin {
 @Id @Column(name="ACCOUNT_ID") private Long accountId; @OneToOne @MapsId @JoinColumn(name="ACCOUNT_ID") private Account account;
 @Column(name="PIN_HASH",nullable=false) private String pinHash; @Column(name="FAILED_ATTEMPTS",nullable=false) private int failedAttempts; @Column(name="LOCKED_UNTIL") private LocalDateTime lockedUntil;
 protected AccountPin(){} public AccountPin(Account a,String hash){account=a;pinHash=hash;} public String getPinHash(){return pinHash;} public int getFailedAttempts(){return failedAttempts;} public LocalDateTime getLockedUntil(){return lockedUntil;} public void failed(){failedAttempts++; if(failedAttempts>=3) lockedUntil=LocalDateTime.now().plusMinutes(15);} public void reset(){failedAttempts=0;lockedUntil=null;}
}

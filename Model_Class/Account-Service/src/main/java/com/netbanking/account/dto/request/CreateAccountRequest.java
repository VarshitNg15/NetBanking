package com.netbanking.account.dto.request;
import com.netbanking.account.entity.AccountType; import jakarta.validation.constraints.*;
public record CreateAccountRequest(@NotBlank @Size(max=50) String customerId, @NotNull AccountType accountType, @Pattern(regexp="[A-Z]{3}") String currencyCode) {}

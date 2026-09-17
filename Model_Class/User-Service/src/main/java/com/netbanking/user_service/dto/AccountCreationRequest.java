package com.netbanking.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreationRequest {

    @NotBlank
    @Size(max = 50)
    private String customerId;

    @NotBlank
    @Size(max = 20)
    @Pattern(
            regexp = "SAVINGS|CURRENT",
            message = "Account type must be SAVINGS or CURRENT"
    )
    private String accountType;
}
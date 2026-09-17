package com.netbanking.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOpeningRequestDto {

    @NotBlank(message = "Request ID is required")
    @Size(max = 30)
    private String requestId;

    @NotBlank(message = "Customer ID is required")
    @Size(max = 50)
    private String customerId;

    @NotEmpty(message = "At least one account type is required")
    private List<String> accountTypes;
}
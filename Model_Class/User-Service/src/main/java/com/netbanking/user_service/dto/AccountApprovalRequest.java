package com.netbanking.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountApprovalRequest {

    @NotBlank(message = "Admin ID is required")
    @Size(max = 50)
    private String adminId;

    @Size(max = 500)
    private String reason;
}
package com.netbanking.user_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerRequest {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 50, message = "Customer ID cannot exceed 50 characters")
    private String customerId;
}
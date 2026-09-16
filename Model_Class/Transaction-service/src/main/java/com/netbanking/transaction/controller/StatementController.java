package com.netbanking.transaction.controller;

import com.netbanking.transaction.dto.request.StatementRequestDto;
import com.netbanking.transaction.dto.response.StatementResponse;
import com.netbanking.transaction.service.StatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    @PostMapping
    public ResponseEntity<StatementResponse> requestStatement(
            @Valid @RequestBody StatementRequestDto request,
            @RequestHeader(value = "X-Customer-Id") String customerId,
            @RequestHeader(value = "X-Initiated-By") String requestedBy) {

        return ResponseEntity.ok(statementService.requestStatement(request, customerId, requestedBy));
    }
}

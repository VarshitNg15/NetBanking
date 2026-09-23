package com.netbanking.transaction.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netbanking.transaction.dto.request.StatementRequestDto;
import com.netbanking.transaction.dto.response.StatementResponse;
import com.netbanking.transaction.service.StatementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/statements", "/api/statements", "/api/v1/transactions/statements", "/api/transactions/statements"})
@RequiredArgsConstructor
@Tag(name = "Account Statements", description = "Endpoints for generating, inquiring, and downloading account statements")
public class StatementController {

    private final StatementService statementService;

    @PostMapping
    @Operation(summary = "Request statement", description = "Initiates statement generation for a specified account and date range.")
    public ResponseEntity<StatementResponse> requestStatement(
            @Valid @RequestBody StatementRequestDto request,
            @RequestHeader(value = "X-Customer-Id") String customerId,
            @RequestHeader(value = "X-Initiated-By", required = false, defaultValue = "CUSTOMER") String requestedBy) {

        return ResponseEntity.ok(statementService.requestStatement(request, customerId, requestedBy));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get statement request status", description = "Retrieves current status and metadata of a statement request.")
    public ResponseEntity<StatementResponse> getStatement(
            @PathVariable Long requestId,
            @RequestHeader(value = "X-Customer-Id") String customerId) {
        return ResponseEntity.ok(statementService.getStatement(requestId, customerId));
    }

    @GetMapping
    @Operation(summary = "List customer statements", description = "Returns all statement requests belonging to the authenticated customer.")
    public ResponseEntity<List<StatementResponse>> listCustomerStatements(
            @RequestHeader(value = "X-Customer-Id") String customerId) {
        return ResponseEntity.ok(statementService.getStatementsByCustomer(customerId));
    }

    @GetMapping(value = "/{requestId}/download", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Download statement content", description = "Downloads the generated statement as formatted text or CSV.")
    public ResponseEntity<String> downloadStatement(
            @PathVariable Long requestId,
            @RequestHeader(value = "X-Customer-Id") String customerId) {
        String content = statementService.generateDownloadContent(requestId, customerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement-" + requestId + ".txt\"")
                .body(content);
    }
}

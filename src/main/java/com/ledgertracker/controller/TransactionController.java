package com.ledgertracker.controller;

import com.ledgertracker.dto.ApiResponse;
import com.ledgertracker.dto.PagedResponse;
import com.ledgertracker.dto.TransactionRequest;
import com.ledgertracker.model.Transaction;
import com.ledgertracker.service.TransactionService;
import com.ledgertracker.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Transaction>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "t.transaction_date") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        PagedResponse<Transaction> result = transactionService.getTransactions(
                principal.getId(), startDate, endDate, categoryId, type,
                minAmount, maxAmount, search, sortBy, sortDirection, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> getTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        Transaction tx = transactionService.getTransaction(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(tx));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionRequest request
    ) {
        Transaction created = transactionService.createTransaction(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Entry added to ledger successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> updateTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request
    ) {
        Transaction updated = transactionService.updateTransaction(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Ledger entry updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        transactionService.deleteTransaction(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Ledger entry removed successfully", null));
    }
}

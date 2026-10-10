package com.ledgertracker.controller;

import com.ledgertracker.dto.ApiResponse;
import com.ledgertracker.dto.BudgetProgressDto;
import com.ledgertracker.dto.BudgetRequest;
import com.ledgertracker.service.BudgetService;
import com.ledgertracker.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetProgressDto>>> getBudgets(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        List<BudgetProgressDto> budgets = budgetService.getBudgetsWithProgress(principal.getId(), targetYear, targetMonth);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetProgressDto>> setBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BudgetRequest request
    ) {
        BudgetProgressDto progress = budgetService.setBudget(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Budget envelope established successfully", progress));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        budgetService.deleteBudget(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Budget envelope removed successfully", null));
    }
}

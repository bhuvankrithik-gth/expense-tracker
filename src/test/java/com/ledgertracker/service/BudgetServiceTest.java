package com.ledgertracker.service;

import com.ledgertracker.dto.BudgetProgressDto;
import com.ledgertracker.dto.BudgetRequest;
import com.ledgertracker.exception.BadRequestException;
import com.ledgertracker.model.Category;
import com.ledgertracker.repository.BudgetRepository;
import com.ledgertracker.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository, categoryRepository);
    }

    @Test
    void setBudget_ShouldReject_WhenCategoryIsIncomeType() {
        Long userId = 1L;
        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(20L);
        request.setMonth(10);
        request.setYear(2026);
        request.setAmount(new BigDecimal("1000.00"));

        Category incomeCategory = new Category();
        incomeCategory.setId(20L);
        incomeCategory.setType("INCOME");

        when(categoryRepository.findByIdAndUserId(20L, userId)).thenReturn(Optional.of(incomeCategory));

        assertThrows(BadRequestException.class, () -> budgetService.setBudget(userId, request));
    }

    @Test
    void getBudgetsWithProgress_ShouldReturnCalculatedList() {
        Long userId = 1L;
        int year = 2026;
        int month = 10;

        BudgetProgressDto dto = new BudgetProgressDto();
        dto.setBudgetId(1L);
        dto.setCategoryId(5L);
        dto.setCategoryName("Groceries");
        dto.setBudgetAmount(new BigDecimal("500.00"));
        dto.setActualSpent(new BigDecimal("350.00"));
        dto.setRemainingAmount(new BigDecimal("150.00"));
        dto.setPercentageUsed(new BigDecimal("70.00"));
        dto.setBudgetStatus("SAFE");

        when(budgetRepository.findBudgetsWithProgress(userId, year, month))
                .thenReturn(Collections.singletonList(dto));

        List<BudgetProgressDto> results = budgetService.getBudgetsWithProgress(userId, year, month);

        assertEquals(1, results.size());
        assertEquals("SAFE", results.get(0).getBudgetStatus());
        assertEquals(new BigDecimal("70.00"), results.get(0).getPercentageUsed());
    }
}

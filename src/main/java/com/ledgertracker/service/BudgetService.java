package com.ledgertracker.service;

import com.ledgertracker.dto.BudgetProgressDto;
import com.ledgertracker.dto.BudgetRequest;
import com.ledgertracker.exception.BadRequestException;
import com.ledgertracker.exception.ResourceNotFoundException;
import com.ledgertracker.model.Budget;
import com.ledgertracker.model.Category;
import com.ledgertracker.repository.BudgetRepository;
import com.ledgertracker.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<BudgetProgressDto> getBudgetsWithProgress(Long userId, int year, int month) {
        return budgetRepository.findBudgetsWithProgress(userId, year, month);
    }

    @Transactional
    public BudgetProgressDto setBudget(Long userId, BudgetRequest request) {
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new BadRequestException("Invalid category selected. Category not found."));

        if (!"EXPENSE".equalsIgnoreCase(category.getType())) {
            throw new BadRequestException("Budgets can only be set for Expense categories.");
        }

        Optional<Budget> existingOpt = budgetRepository.findByUserCategoryAndPeriod(
                userId, request.getCategoryId(), request.getMonth(), request.getYear()
        );

        if (existingOpt.isPresent()) {
            Budget existing = existingOpt.get();
            existing.setAmount(request.getAmount());
            budgetRepository.update(existing);
        } else {
            Budget budget = new Budget();
            budget.setUserId(userId);
            budget.setCategoryId(request.getCategoryId());
            budget.setMonth(request.getMonth());
            budget.setYear(request.getYear());
            budget.setAmount(request.getAmount());
            budgetRepository.create(budget);
        }

        // Return updated progress for this period
        return budgetRepository.findBudgetsWithProgress(userId, request.getYear(), request.getMonth())
                .stream()
                .filter(b -> b.getCategoryId().equals(request.getCategoryId()))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void deleteBudget(Long userId, Long id) {
        budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget envelope not found with id: " + id));

        budgetRepository.delete(id, userId);
    }
}

package com.ledgertracker.service;

import com.ledgertracker.dto.BudgetProgressDto;
import com.ledgertracker.dto.CategorySpendingDto;
import com.ledgertracker.dto.DashboardSummaryDto;
import com.ledgertracker.dto.MonthlySummaryDto;
import com.ledgertracker.dto.MonthlyTrendDto;
import com.ledgertracker.model.Transaction;
import com.ledgertracker.repository.BudgetRepository;
import com.ledgertracker.repository.DashboardRepository;
import com.ledgertracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public DashboardService(DashboardRepository dashboardRepository,
                            TransactionRepository transactionRepository,
                            BudgetRepository budgetRepository) {
        this.dashboardRepository = dashboardRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    public DashboardSummaryDto getDashboardSummary(Long userId, int year, int month) {
        MonthlySummaryDto currentMonth = dashboardRepository.getMonthlySummary(userId, year, month);
        List<Transaction> recentTransactions = transactionRepository.findRecent(userId, 8);
        List<CategorySpendingDto> categorySpending = dashboardRepository.getCategorySpending(userId, year, month);
        List<BudgetProgressDto> budgets = budgetRepository.findBudgetsWithProgress(userId, year, month);
        List<MonthlyTrendDto> sixMonthTrends = dashboardRepository.getSixMonthTrends(userId, year, month);

        return new DashboardSummaryDto(
                currentMonth,
                recentTransactions,
                categorySpending,
                budgets,
                sixMonthTrends
        );
    }
}

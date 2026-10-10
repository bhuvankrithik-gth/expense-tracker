package com.ledgertracker.dto;

import com.ledgertracker.model.Transaction;

import java.util.List;

public class DashboardSummaryDto {
    private MonthlySummaryDto currentMonth;
    private List<Transaction> recentTransactions;
    private List<CategorySpendingDto> categorySpending;
    private List<BudgetProgressDto> budgets;
    private List<MonthlyTrendDto> sixMonthTrends;

    public DashboardSummaryDto() {
    }

    public DashboardSummaryDto(MonthlySummaryDto currentMonth, List<Transaction> recentTransactions,
                               List<CategorySpendingDto> categorySpending, List<BudgetProgressDto> budgets,
                               List<MonthlyTrendDto> sixMonthTrends) {
        this.currentMonth = currentMonth;
        this.recentTransactions = recentTransactions;
        this.categorySpending = categorySpending;
        this.budgets = budgets;
        this.sixMonthTrends = sixMonthTrends;
    }

    public MonthlySummaryDto getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(MonthlySummaryDto currentMonth) {
        this.currentMonth = currentMonth;
    }

    public List<Transaction> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<Transaction> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    public List<CategorySpendingDto> getCategorySpending() {
        return categorySpending;
    }

    public void setCategorySpending(List<CategorySpendingDto> categorySpending) {
        this.categorySpending = categorySpending;
    }

    public List<BudgetProgressDto> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<BudgetProgressDto> budgets) {
        this.budgets = budgets;
    }

    public List<MonthlyTrendDto> getSixMonthTrends() {
        return sixMonthTrends;
    }

    public void setSixMonthTrends(List<MonthlyTrendDto> sixMonthTrends) {
        this.sixMonthTrends = sixMonthTrends;
    }
}

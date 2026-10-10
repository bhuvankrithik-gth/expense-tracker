package com.ledgertracker.dto;

import java.math.BigDecimal;

public class MonthlyTrendDto {
    private Integer year;
    private Integer month;
    private String monthLabel; // e.g. "May 2026", "Jun 2026"
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal netSavings;

    public MonthlyTrendDto() {
        this.income = BigDecimal.ZERO;
        this.expense = BigDecimal.ZERO;
        this.netSavings = BigDecimal.ZERO;
    }

    public MonthlyTrendDto(Integer year, Integer month, String monthLabel, BigDecimal income, BigDecimal expense, BigDecimal netSavings) {
        this.year = year;
        this.month = month;
        this.monthLabel = monthLabel;
        this.income = income != null ? income : BigDecimal.ZERO;
        this.expense = expense != null ? expense : BigDecimal.ZERO;
        this.netSavings = netSavings != null ? netSavings : BigDecimal.ZERO;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }
}

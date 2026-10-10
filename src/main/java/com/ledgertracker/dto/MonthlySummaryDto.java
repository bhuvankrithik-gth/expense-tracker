package com.ledgertracker.dto;

import java.math.BigDecimal;

public class MonthlySummaryDto {
    private Integer year;
    private Integer month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netSavings;
    private Long transactionCount;

    public MonthlySummaryDto() {
        this.totalIncome = BigDecimal.ZERO;
        this.totalExpense = BigDecimal.ZERO;
        this.netSavings = BigDecimal.ZERO;
        this.transactionCount = 0L;
    }

    public MonthlySummaryDto(Integer year, Integer month, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netSavings, Long transactionCount) {
        this.year = year;
        this.month = month;
        this.totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        this.totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
        this.netSavings = netSavings != null ? netSavings : BigDecimal.ZERO;
        this.transactionCount = transactionCount != null ? transactionCount : 0L;
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

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }
}

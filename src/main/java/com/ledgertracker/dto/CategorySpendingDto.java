package com.ledgertracker.dto;

import java.math.BigDecimal;

public class CategorySpendingDto {
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private BigDecimal totalSpent;
    private Long transactionCount;
    private BigDecimal averageAmount;
    private BigDecimal percentageOfTotal;

    public CategorySpendingDto() {
        this.totalSpent = BigDecimal.ZERO;
        this.transactionCount = 0L;
        this.averageAmount = BigDecimal.ZERO;
        this.percentageOfTotal = BigDecimal.ZERO;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }

    public void setAverageAmount(BigDecimal averageAmount) {
        this.averageAmount = averageAmount;
    }

    public BigDecimal getPercentageOfTotal() {
        return percentageOfTotal;
    }

    public void setPercentageOfTotal(BigDecimal percentageOfTotal) {
        this.percentageOfTotal = percentageOfTotal;
    }
}

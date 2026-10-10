package com.ledgertracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    @NotNull(message = "Category selection is required")
    private Long categoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "EXPENSE|INCOME", message = "Type must be either EXPENSE or INCOME")
    private String type;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "CASH|DEBIT_CARD|CREDIT_CARD|BANK_TRANSFER|UPI|OTHER",
             message = "Invalid payment method")
    private String paymentMethod;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    public TransactionRequest() {
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note != null ? note.trim() : null;
    }
}

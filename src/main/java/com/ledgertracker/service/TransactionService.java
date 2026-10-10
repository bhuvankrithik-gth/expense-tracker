package com.ledgertracker.service;

import com.ledgertracker.dto.PagedResponse;
import com.ledgertracker.dto.TransactionRequest;
import com.ledgertracker.exception.BadRequestException;
import com.ledgertracker.exception.ResourceNotFoundException;
import com.ledgertracker.model.Category;
import com.ledgertracker.model.Transaction;
import com.ledgertracker.repository.CategoryRepository;
import com.ledgertracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Transaction createTransaction(Long userId, TransactionRequest request) {
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new BadRequestException("Invalid category selected. Category not found."));

        if (!category.getType().equalsIgnoreCase(request.getType())) {
            throw new BadRequestException("Category type mismatch: Category is " + category.getType() + " but transaction type is " + request.getType());
        }

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setCategoryId(category.getId());
        tx.setAmount(request.getAmount());
        tx.setType(request.getType().toUpperCase());
        tx.setTransactionDate(request.getTransactionDate());
        tx.setPaymentMethod(request.getPaymentMethod().toUpperCase());
        tx.setNote(request.getNote());

        Transaction created = transactionRepository.create(tx);
        return transactionRepository.findByIdAndUserId(created.getId(), userId)
                .orElse(created);
    }

    @Transactional
    public Transaction updateTransaction(Long userId, Long id, TransactionRequest request) {
        Transaction existing = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new BadRequestException("Invalid category selected. Category not found."));

        if (!category.getType().equalsIgnoreCase(request.getType())) {
            throw new BadRequestException("Category type mismatch: Category is " + category.getType() + " but transaction type is " + request.getType());
        }

        existing.setCategoryId(category.getId());
        existing.setAmount(request.getAmount());
        existing.setType(request.getType().toUpperCase());
        existing.setTransactionDate(request.getTransactionDate());
        existing.setPaymentMethod(request.getPaymentMethod().toUpperCase());
        existing.setNote(request.getNote());

        transactionRepository.update(existing);

        return transactionRepository.findByIdAndUserId(id, userId)
                .orElse(existing);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long id) {
        Transaction existing = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        transactionRepository.delete(id, userId);
    }

    public Transaction getTransaction(Long userId, Long id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    public List<Transaction> getRecentTransactions(Long userId, int limit) {
        return transactionRepository.findRecent(userId, limit);
    }

    public PagedResponse<Transaction> getTransactions(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            String type,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        return transactionRepository.findWithFilters(
                userId, startDate, endDate, categoryId, type,
                minAmount, maxAmount, search, sortBy, sortDirection, page, size
        );
    }
}

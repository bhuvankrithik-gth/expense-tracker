package com.ledgertracker.service;

import com.ledgertracker.dto.TransactionRequest;
import com.ledgertracker.exception.BadRequestException;
import com.ledgertracker.exception.ResourceNotFoundException;
import com.ledgertracker.model.Category;
import com.ledgertracker.model.Transaction;
import com.ledgertracker.repository.CategoryRepository;
import com.ledgertracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, categoryRepository);
    }

    @Test
    void createTransaction_ShouldSucceed_WhenCategoryAndTypeMatch() {
        Long userId = 1L;
        TransactionRequest request = new TransactionRequest();
        request.setCategoryId(10L);
        request.setAmount(new BigDecimal("125.50"));
        request.setType("EXPENSE");
        request.setTransactionDate(LocalDate.now());
        request.setPaymentMethod("DEBIT_CARD");
        request.setNote("Provisions at grocery");

        Category category = new Category();
        category.setId(10L);
        category.setName("Groceries");
        category.setType("EXPENSE");

        when(categoryRepository.findByIdAndUserId(10L, userId)).thenReturn(Optional.of(category));

        Transaction createdTx = new Transaction();
        createdTx.setId(100L);
        createdTx.setUserId(userId);
        createdTx.setCategoryId(10L);
        createdTx.setAmount(new BigDecimal("125.50"));
        createdTx.setType("EXPENSE");

        when(transactionRepository.create(any(Transaction.class))).thenReturn(createdTx);
        when(transactionRepository.findByIdAndUserId(100L, userId)).thenReturn(Optional.of(createdTx));

        Transaction result = transactionService.createTransaction(userId, request);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(transactionRepository, times(1)).create(any(Transaction.class));
    }

    @Test
    void createTransaction_ShouldFail_WhenTypeMismatchesCategory() {
        Long userId = 1L;
        TransactionRequest request = new TransactionRequest();
        request.setCategoryId(10L);
        request.setAmount(new BigDecimal("125.50"));
        request.setType("INCOME"); // mismatched with EXPENSE category
        request.setTransactionDate(LocalDate.now());
        request.setPaymentMethod("BANK_TRANSFER");

        Category category = new Category();
        category.setId(10L);
        category.setName("Groceries");
        category.setType("EXPENSE");

        when(categoryRepository.findByIdAndUserId(10L, userId)).thenReturn(Optional.of(category));

        assertThrows(BadRequestException.class, () -> transactionService.createTransaction(userId, request));
        verify(transactionRepository, never()).create(any(Transaction.class));
    }

    @Test
    void deleteTransaction_ShouldThrowNotFound_WhenTransactionDoesNotExist() {
        Long userId = 1L;
        Long txId = 999L;

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.deleteTransaction(userId, txId));
        verify(transactionRepository, never()).delete(anyLong(), anyLong());
    }
}

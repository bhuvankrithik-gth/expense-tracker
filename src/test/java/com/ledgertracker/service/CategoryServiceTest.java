package com.ledgertracker.service;

import com.ledgertracker.dto.CategoryRequest;
import com.ledgertracker.exception.BadRequestException;
import com.ledgertracker.model.Category;
import com.ledgertracker.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void createCategory_ShouldSucceed_WhenNameIsUnique() {
        Long userId = 1L;
        CategoryRequest request = new CategoryRequest("Fine Dining", "EXPENSE", "#1B4332", "coffee");

        when(categoryRepository.existsByNameAndType(userId, "Fine Dining", "EXPENSE")).thenReturn(false);

        Category created = new Category();
        created.setId(50L);
        created.setUserId(userId);
        created.setName("Fine Dining");
        created.setType("EXPENSE");

        when(categoryRepository.create(any(Category.class))).thenReturn(created);

        Category result = categoryService.createCategory(userId, request);

        assertNotNull(result);
        assertEquals("Fine Dining", result.getName());
        verify(categoryRepository, times(1)).create(any(Category.class));
    }

    @Test
    void createCategory_ShouldThrowBadRequest_WhenDuplicateNameExists() {
        Long userId = 1L;
        CategoryRequest request = new CategoryRequest("Groceries", "EXPENSE", "#1B4332", "cart");

        when(categoryRepository.existsByNameAndType(userId, "Groceries", "EXPENSE")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.createCategory(userId, request));
        verify(categoryRepository, never()).create(any(Category.class));
    }

    @Test
    void deleteCategory_ShouldFail_WhenCategoryHasActiveTransactions() {
        Long userId = 1L;
        Long catId = 10L;

        Category existing = new Category();
        existing.setId(catId);
        existing.setUserId(userId);
        existing.setDefault(false);

        when(categoryRepository.findByIdAndUserId(catId, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.hasTransactions(catId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory(userId, catId));
        verify(categoryRepository, never()).delete(anyLong(), anyLong());
    }
}

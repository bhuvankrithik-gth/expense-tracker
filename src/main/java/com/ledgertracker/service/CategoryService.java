package com.ledgertracker.service;

import com.ledgertracker.dto.CategoryRequest;
import com.ledgertracker.exception.BadRequestException;
import com.ledgertracker.exception.ResourceNotFoundException;
import com.ledgertracker.model.Category;
import com.ledgertracker.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getCategories(Long userId) {
        return categoryRepository.findByUserId(userId);
    }

    public List<Category> getCategoriesByType(Long userId, String type) {
        return categoryRepository.findByUserIdAndType(userId, type);
    }

    @Transactional
    public Category createCategory(Long userId, CategoryRequest request) {
        if (categoryRepository.existsByNameAndType(userId, request.getName(), request.getType())) {
            throw new BadRequestException("A category named '" + request.getName() + "' already exists for " + request.getType().toLowerCase() + ".");
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName());
        category.setType(request.getType());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setDefault(false);

        return categoryRepository.create(category);
    }

    @Transactional
    public Category updateCategory(Long userId, Long id, CategoryRequest request) {
        Category existing = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (existing.isDefault() || existing.getUserId() == null) {
            throw new BadRequestException("Default system categories cannot be modified. Create a custom category instead.");
        }

        existing.setName(request.getName());
        existing.setColor(request.getColor());
        existing.setIcon(request.getIcon());

        categoryRepository.update(existing);
        return existing;
    }

    @Transactional
    public void deleteCategory(Long userId, Long id) {
        Category existing = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (existing.isDefault() || existing.getUserId() == null) {
            throw new BadRequestException("Default system categories cannot be deleted.");
        }

        if (categoryRepository.hasTransactions(id)) {
            throw new BadRequestException("Category cannot be deleted while transactions are assigned to it. Please reassign or delete associated transactions first.");
        }

        categoryRepository.delete(id, userId);
    }
}

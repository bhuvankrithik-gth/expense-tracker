package com.ledgertracker.controller;

import com.ledgertracker.dto.ApiResponse;
import com.ledgertracker.dto.CategoryRequest;
import com.ledgertracker.model.Category;
import com.ledgertracker.service.CategoryService;
import com.ledgertracker.service.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type
    ) {
        List<Category> categories;
        if (type != null && !type.isBlank()) {
            categories = categoryService.getCategoriesByType(principal.getId(), type.toUpperCase());
        } else {
            categories = categoryService.getCategories(principal.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CategoryRequest request
    ) {
        Category created = categoryService.createCategory(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        Category updated = categoryService.updateCategory(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Category removed successfully", null));
    }
}

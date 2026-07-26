package com.example.secdsp.modules.category.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.category.dto.request.CreateCategoryRequest;
import com.example.secdsp.modules.category.dto.request.UpdateCategoryRequest;
import com.example.secdsp.modules.category.dto.response.CategoryResponse;
import com.example.secdsp.modules.category.dto.response.CategoryTreeResponse;
import com.example.secdsp.modules.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SELLER','MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(ApiResponse.success("Category created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.ok(
            ApiResponse.success("Category deleted successfully")
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> getCategories(
        @RequestParam(value = "keyword", required = false) String keyword,
        Pageable pageable
    ) {
        Page<CategoryResponse> responsePage = categoryService.getCategories(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> response = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

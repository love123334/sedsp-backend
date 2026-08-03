package com.example.secdsp.modules.category.controller;

import com.example.secdsp.common.api.BaseResponse;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(BaseResponse.success("Category created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<CategoryResponse>> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(BaseResponse.success("Category updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteCategory(@PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.ok(
            BaseResponse.success("Category deleted successfully")
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<Page<CategoryResponse>>> getCategories(
        @RequestParam(value = "keyword", required = false) String keyword,
        Pageable pageable
    ) {
        Page<CategoryResponse> responsePage = categoryService.getCategories(keyword, pageable);
        return ResponseEntity.ok(BaseResponse.success(responsePage));
    }

    @GetMapping("/tree")
    public ResponseEntity<BaseResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> response = categoryService.getCategoryTree();
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}

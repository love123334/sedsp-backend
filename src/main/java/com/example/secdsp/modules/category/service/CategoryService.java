package com.example.secdsp.modules.category.service;

import com.example.secdsp.modules.category.dto.request.CreateCategoryRequest;
import com.example.secdsp.modules.category.dto.request.UpdateCategoryRequest;
import com.example.secdsp.modules.category.dto.response.CategoryResponse;
import com.example.secdsp.modules.category.dto.response.CategoryTreeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);
    void deleteCategory(Long id);
    CategoryResponse getCategoryById(Long id);
    Page<CategoryResponse> getCategories(String keyword, Pageable pageable);
    List<CategoryTreeResponse> getCategoryTree();
}

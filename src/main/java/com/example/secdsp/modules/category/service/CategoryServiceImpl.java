package com.example.secdsp.modules.category.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.modules.category.dto.request.CreateCategoryRequest;
import com.example.secdsp.modules.category.dto.request.UpdateCategoryRequest;
import com.example.secdsp.modules.category.dto.response.CategoryResponse;
import com.example.secdsp.modules.category.dto.response.CategoryTreeResponse;
import com.example.secdsp.modules.category.entity.Category;
import com.example.secdsp.modules.category.mapper.CategoryMapper;
import com.example.secdsp.modules.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Attempting to create category with name: {}", request.getName());

        validateCategoryUniqueness(request.getName(), request.getSlug(), null);

        Category category = categoryMapper.toEntity(request);

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findByIdAndDeletedAtIsNull(request.getParentId())
                    .orElseThrow(() -> new BusinessException("Parent category not found with ID: " + request.getParentId()));
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        log.info("Attempting to update category with ID: {}", id);
        Category existingCategory = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        validateCategoryUniqueness(request.getName(), request.getSlug(), id);

        Category parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException("Category cannot be its own parent.");
            }
            parent = categoryRepository.findByIdAndDeletedAtIsNull(request.getParentId())
                    .orElseThrow(() -> new BusinessException("Parent category not found with ID: " + request.getParentId()));

            // Prevent circular hierarchy
            if (isCircular(parent, existingCategory)) {
                throw new BusinessException("Circular hierarchy detected. Cannot assign category as child of its own descendant.");
            }
        }
        categoryMapper.updateEntityFromDto(request, existingCategory);
        existingCategory.setParent(parent);

        Category updatedCategory = categoryRepository.save(existingCategory);
        log.info("Category updated successfully with ID: {}", updatedCategory.getId());
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Attempting to soft delete category with ID: {}", id);
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        // Check for active products
        long activeProductsCount = categoryRepository.countActiveProductsByCategoryId(id);
        if (activeProductsCount > 0) {
            throw new BusinessException("Cannot delete category as it has active products associated with it.");
        }

        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
        log.info("Category with ID {} soft deleted successfully.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Fetching category by ID: {}", id);
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategories(String keyword, Pageable pageable) {
        log.debug("Fetching categories with keyword: {} and pageable: {}", keyword, pageable);
        return categoryRepository.searchCategories(keyword, pageable)
                .map(categoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        log.debug("Fetching category tree.");
        List<Category> allCategories = categoryRepository.findAllActiveCategories();
        Map<Long, Category> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<CategoryTreeResponse> rootCategoryTreeResponses = new ArrayList<>();

        for (Category category : allCategories) {
            if (category.getParent() == null) {
                // This is a root category, build its subtree
                rootCategoryTreeResponses.add(buildCategoryTree(category, categoryMap));
            }
        }
        return rootCategoryTreeResponses;
    }

    private CategoryTreeResponse buildCategoryTree(Category category, Map<Long, Category> categoryMap) {
        CategoryTreeResponse treeResponse = categoryMapper.toTreeResponse(category);
        List<CategoryTreeResponse> childrenTreeResponses = category.getChildren().stream()
                .filter(c -> c.getDeletedAt() == null) // Only include active children
                .map(child -> buildCategoryTree(child, categoryMap))
                .collect(Collectors.toList());
        if (!childrenTreeResponses.isEmpty()) {
            treeResponse.setChildren(childrenTreeResponses);
        }
        return treeResponse;
    }

    private void validateCategoryUniqueness(String name, String slug, Long id) {
        if (id == null) { // Create operation
            if (categoryRepository.existsByNameAndDeletedAtIsNull(name)) {
                throw new BusinessException("Category with name '" + name + "' already exists.");
            }
            if (categoryRepository.existsBySlugAndDeletedAtIsNull(slug)) {
                throw new BusinessException("Category with slug '" + slug + "' already exists.");
            }
        } else { // Update operation
            categoryRepository.findByNameAndIdNotAndDeletedAtIsNull(name, id)
                    .ifPresent(c -> {
                        throw new BusinessException("Category with name '" + name + "' already exists.");
                    });
            categoryRepository.findBySlugAndIdNotAndDeletedAtIsNull(slug, id)
                    .ifPresent(c -> {
                        throw new BusinessException("Category with slug '" + slug + "' already exists.");
                    });
        }
    }

    private boolean isCircular(Category potentialParent, Category child) {
        Set<Long> visited = new java.util.HashSet<>();
        Category current = potentialParent;
        while (current != null) {
            if (current.getId().equals(child.getId())) {
                return true; // Found a circular reference
            }
            if (!visited.add(current.getId())) {
                // This should not happen with a valid parent structure but acts as a safeguard
                log.warn("Circular path detected in parent traversal for category ID: {}", potentialParent.getId());
                return true; // Loop detected during traversal
            }
            current = current.getParent();
        }
        return false;
    }
}

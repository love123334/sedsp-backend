package com.example.secdsp.modules.category.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ErrorCode;
import com.example.secdsp.common.exception.ResourceAlreadyExistsException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.modules.category.dto.internal.CategoryInfo;
import com.example.secdsp.modules.category.dto.request.CreateCategoryRequest;
import com.example.secdsp.modules.category.dto.request.UpdateCategoryRequest;
import com.example.secdsp.modules.category.dto.response.CategoryResponse;
import com.example.secdsp.modules.category.dto.response.CategoryTreeResponse;
import com.example.secdsp.modules.category.entity.Category;
import com.example.secdsp.modules.category.mapper.CategoryMapper;
import com.example.secdsp.modules.category.repository.CategoryRepository;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;
    private final Slugify slugify;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {

        log.info("Attempting to create category with name: {}", request.getName());

        Category category = categoryMapper.toEntity(request);

        category.setSlug(generateUniqueSlug(request.getName(), null));

        validateDuplicateCategoryName(
            request.getName(),
            request.getParentId(),
            null
        );

        try {
            return categoryMapper.toResponse(categoryRepository.save(category));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Slug conflict occurred. Please retry."
            );
        }
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {

        log.info("Attempting to update category with ID: {}", id);

        Category existingCategory = getActiveCategory(id);

        boolean nameChanged = !existingCategory.getName().equals(request.getName());

        boolean parentChanged = !Objects.equals(
            existingCategory.getParent() == null
                ? null
                : existingCategory.getParent().getId(),
            request.getParentId()
        );

        if (nameChanged || parentChanged) {
            validateDuplicateCategoryName(
                request.getName(),
                request.getParentId(),
                id
            );
        }

        categoryMapper.updateEntityFromDto(request, existingCategory);

        if (nameChanged) {
            existingCategory.setSlug(
                generateUniqueSlug(request.getName(), id)
            );
        }

        if (request.getParentId() == null) {

            existingCategory.setParent(null);

        } else {

            if (id.equals(request.getParentId())) {
                throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Category cannot be its own parent."
                );
            }

            Category parent = getActiveCategory(request.getParentId());

            if (isCircular(parent, existingCategory)) {
                throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "Circular hierarchy detected."
                );
            }

            existingCategory.setParent(parent);
        }

        Category updatedCategory = categoryRepository.save(existingCategory);

        log.info("Category updated successfully with ID: {}", updatedCategory.getId());

        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {

        log.info("Attempting to soft delete category with ID: {}", id);

        Category category = getActiveCategory(id);

        if (categoryRepository.existsByParent_Id(id)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Cannot delete category with child categories."
            );
        }

        if (productRepository.existsByCategory_Id(id)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Cannot delete category because it contains products."
            );
        }

        categoryRepository.delete(category);

        log.info("Category with ID {} soft deleted successfully.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Fetching category by ID: {}", id);
        Category category = getActiveCategory(id);
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

        List<Category> categories = categoryRepository.findAllWithParent();

        // Map id → response
        Map<Long, CategoryTreeResponse> responseMap = new HashMap<>();

        for (Category category : categories) {
            responseMap.put(
                category.getId(),
                categoryMapper.toTreeResponse(category)
            );
        }

        List<CategoryTreeResponse> roots = new ArrayList<>();

        for (Category category : categories) {

            CategoryTreeResponse current = responseMap.get(category.getId());

            if (category.getParent() == null) {
                roots.add(current);
            } else {
                CategoryTreeResponse parent =
                    responseMap.get(category.getParent().getId());

                parent.getChildren().add(current);
            }
        }

        return roots;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryInfo getCategoryInfo(Long id) {

        Category category = getActiveCategory(id);

        return new CategoryInfo(
            category.getId(),
            category.getName(),
            category.getSlug()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {

        Category category = categoryRepository.findBySlug(slug)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Category slug", slug));

        return categoryMapper.toResponse(category);
    }

    private boolean isCircular(Category potentialParent, Category child) {
        Set<Long> visited = new HashSet<>();
        Category current = potentialParent;
        while (current != null) {
            if (current.getId().equals(child.getId())) {
                return true;
            }
            if (!visited.add(current.getId())) {
                log.warn("Circular path detected in parent traversal for category ID: {}", potentialParent.getId());
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private Category getActiveCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Category", id));
    }

    private String generateUniqueSlug(String name, Long currentId) {

        String baseSlug = slugify.slugify(name);
        String slug = baseSlug;

        int counter = 1;

        while (true) {

            boolean exists = (currentId == null)
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.findBySlugAndIdNot(slug, currentId).isPresent();

            if (!exists) {
                return slug;
            }

            slug = baseSlug + "-" + counter++;
        }
    }

    private void validateDuplicateCategoryName(
        String name,
        Long parentId,
        Long currentId
    ) {

        boolean exists;

        if (parentId == null) {

            exists = currentId == null
                ? categoryRepository.existsByNameAndParentIsNull(name)
                : categoryRepository
                .findByNameAndParentIsNullAndIdNot(name, currentId)
                .isPresent();

        } else {

            exists = currentId == null
                ? categoryRepository.existsByNameAndParent_Id(name, parentId)
                : categoryRepository
                .findByNameAndParent_IdAndIdNot(name, parentId, currentId)
                .isPresent();
        }

        if (exists) {

            String message =
                parentId == null
                    ? "Root category name already exists."
                    : "Category name already exists in this parent.";

            throw new ResourceAlreadyExistsException(message);
        }
    }
}

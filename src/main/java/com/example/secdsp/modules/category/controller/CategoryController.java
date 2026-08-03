package com.example.secdsp.modules.category.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.category.dto.request.CreateCategoryRequest;
import com.example.secdsp.modules.category.dto.request.UpdateCategoryRequest;
import com.example.secdsp.modules.category.dto.response.CategoryResponse;
import com.example.secdsp.modules.category.dto.response.CategoryTreeResponse;
import com.example.secdsp.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
    name = "Category Management",
    description = "APIs for managing product categories"
)
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
        summary = "Create category",
        description = "Create a new product category. Administrator only."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "409", description = "Category already exists", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(
        @Valid @RequestBody CreateCategoryRequest request
    ) {

        CategoryResponse response = categoryService.createCategory(request);

        return new ResponseEntity<>(
            BaseResponse.success("Category created successfully", response),
            HttpStatus.CREATED
        );
    }

    @Operation(
        summary = "Update category",
        description = "Update an existing product category. Administrator only."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<CategoryResponse>> updateCategory(

        @Parameter(
            description = "Category identifier",
            example = "1"
        )
        @PathVariable Long id,

        @Valid @RequestBody UpdateCategoryRequest request
    ) {

        CategoryResponse response = categoryService.updateCategory(id, request);

        return ResponseEntity.ok(
            BaseResponse.success("Category updated successfully", response)
        );
    }

    @Operation(
        summary = "Delete category",
        description = "Delete a category. Administrator only."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteCategory(

        @Parameter(
            description = "Category identifier",
            example = "1"
        )
        @PathVariable Long id
    ) {

        categoryService.deleteCategory(id);

        return ResponseEntity.ok(
            BaseResponse.success("Category deleted successfully")
        );
    }

    @Operation(
        summary = "Get category by ID",
        description = "Retrieve detailed information about a category."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategoryById(

        @Parameter(
            description = "Category identifier",
            example = "1"
        )
        @PathVariable Long id
    ) {

        CategoryResponse response = categoryService.getCategoryById(id);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
        summary = "Get categories",
        description = "Retrieve categories with pagination and optional keyword filtering."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<Page<CategoryResponse>>> getCategories(

        @Parameter(
            description = "Search keyword",
            example = "Shoes"
        )
        @RequestParam(value = "keyword", required = false)
        String keyword,

        Pageable pageable
    ) {

        Page<CategoryResponse> responsePage =
            categoryService.getCategories(keyword, pageable);

        return ResponseEntity.ok(
            BaseResponse.success(responsePage)
        );
    }

    @Operation(
        summary = "Get category tree",
        description = "Retrieve all categories in a hierarchical tree structure."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category tree retrieved successfully")
    })
    @GetMapping("/tree")
    public ResponseEntity<BaseResponse<List<CategoryTreeResponse>>> getCategoryTree() {

        List<CategoryTreeResponse> response =
            categoryService.getCategoryTree();

        return ResponseEntity.ok(
            BaseResponse.success(response)
        );
    }

}
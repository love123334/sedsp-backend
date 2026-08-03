package com.example.secdsp.modules.product.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.product.dto.request.CreateProductRequest;
import com.example.secdsp.modules.product.dto.request.UpdateProductRequest;
import com.example.secdsp.modules.product.dto.response.PriceHistoryResponse;
import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(
    name = "Product Management",
    description = "APIs for managing products and product information"
)
public class ProductController {

    private final ProductService productService;

    @Operation(
        summary = "Create product",
        description = "Create a new product. Requires ADMIN or SELLER role."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<BaseResponse<ProductResponse>> createProduct(
        @Valid @RequestBody CreateProductRequest request
    ) {

        ProductResponse response = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BaseResponse.success(
                "Product created successfully",
                response
            ));
    }

    @Operation(
        summary = "Update product",
        description = "Update an existing product. Requires ADMIN or SELLER role."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<BaseResponse<ProductResponse>> updateProduct(

        @Parameter(
            description = "Product identifier",
            example = "1"
        )
        @PathVariable Long id,

        @Valid @RequestBody UpdateProductRequest request
    ) {

        ProductResponse response = productService.updateProduct(id, request);

        return ResponseEntity.ok(
            BaseResponse.success(
                "Product updated successfully",
                response
            )
        );
    }

    @Operation(
        summary = "Delete product",
        description = "Delete a product. Requires ADMIN or SELLER role."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<BaseResponse<Void>> deleteProduct(

        @Parameter(
            description = "Product identifier",
            example = "1"
        )
        @PathVariable Long id
    ) {

        productService.deleteProduct(id);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(BaseResponse.success("Product deleted successfully"));
    }

    @Operation(
        summary = "Get product details",
        description = "Retrieve detailed information about a product."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ProductDetailResponse>> getProductById(

        @Parameter(
            description = "Product identifier",
            example = "1"
        )
        @PathVariable Long id
    ) {

        ProductDetailResponse response = productService.getProductById(id);

        return ResponseEntity.ok(
            BaseResponse.success(response)
        );
    }

    @Operation(
        summary = "Search products",
        description = "Retrieve products with optional filters and pagination."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<Page<ProductResponse>>> getProducts(

        @Parameter(
            description = "Search keyword",
            example = "iphone"
        )
        @RequestParam(required = false)
        String keyword,

        @Parameter(
            description = "Category identifier",
            example = "3"
        )
        @RequestParam(required = false)
        Long categoryId,

        @Parameter(
            description = "Seller identifier",
            example = "5"
        )
        @RequestParam(required = false)
        Long sellerId,

        @ParameterObject
        @PageableDefault(
            size = 10,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        )
        Pageable pageable
    ) {

        Page<ProductResponse> responsePage =
            productService.getProducts(
                keyword,
                categoryId,
                sellerId,
                pageable
            );

        return ResponseEntity.ok(
            BaseResponse.success(responsePage)
        );
    }

    @Operation(
        summary = "Get price history",
        description = "Retrieve the price history of a product. Requires ADMIN or SELLER role."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Price history retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/{id}/price-history")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<BaseResponse<List<PriceHistoryResponse>>> getPriceHistory(

        @Parameter(
            description = "Product identifier",
            example = "1"
        )
        @PathVariable Long id
    ) {

        return ResponseEntity.ok(
            BaseResponse.success(
                productService.getPriceHistory(id)
            )
        );
    }
}
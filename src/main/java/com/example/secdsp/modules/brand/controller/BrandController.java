package com.example.secdsp.modules.brand.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.brand.dto.request.CreateBrandRequest;
import com.example.secdsp.modules.brand.dto.request.UpdateBrandRequest;
import com.example.secdsp.modules.brand.dto.response.BrandResponse;
import com.example.secdsp.modules.brand.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        BrandResponse response = brandService.createBrand(request);
        return new ResponseEntity<>(ApiResponse.success("Brand created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
        @PathVariable Long id,
        @Valid @RequestBody UpdateBrandRequest request
    ) {
        BrandResponse response = brandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponse.success("Brand updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(
            ApiResponse.success("Brand deleted successfully")
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable Long id) {
        BrandResponse response = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<BrandResponse>>> getBrands(
        @RequestParam(value = "keyword", required = false) String keyword,
        Pageable pageable
    ) {
        Page<BrandResponse> responsePage = brandService.getBrands(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }
}

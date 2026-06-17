package com.example.secdsp.modules.brand.service;

import com.example.secdsp.modules.brand.dto.request.CreateBrandRequest;
import com.example.secdsp.modules.brand.dto.request.UpdateBrandRequest;
import com.example.secdsp.modules.brand.dto.response.BrandResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandService {
    BrandResponse createBrand(CreateBrandRequest request);
    BrandResponse updateBrand(Long id, UpdateBrandRequest request);
    void deleteBrand(Long id);
    BrandResponse getBrandById(Long id);
    Page<BrandResponse> getBrands(String keyword, Pageable pageable);
}

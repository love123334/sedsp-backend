package com.example.secdsp.modules.product.service;

import com.example.secdsp.modules.product.dto.request.CreateProductRequest;
import com.example.secdsp.modules.product.dto.request.UpdateProductRequest;
import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse updateProduct(Long id, UpdateProductRequest request);
    void deleteProduct(Long id);
    ProductDetailResponse getProductById(Long id);
    Page<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            Long sellerId,
            Pageable pageable
    );
}

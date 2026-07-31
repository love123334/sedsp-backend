package com.example.secdsp.modules.product.service;

import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.dto.internal.ProductSummaryInfo;
import com.example.secdsp.modules.product.dto.internal.PriceHistoryInfo;
import com.example.secdsp.modules.product.dto.request.CreateProductRequest;
import com.example.secdsp.modules.product.dto.request.UpdateProductRequest;
import com.example.secdsp.modules.product.dto.response.PriceHistoryResponse;
import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.time.LocalDate;

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

    ProductInfo getProductInfo(Long id);

    List<PriceHistoryResponse> getPriceHistory(Long productId);

    List<PriceHistoryInfo> getPriceHistoryInfo(
        Long productId,
        LocalDate fromDate,
        LocalDate toDate
    );

    ProductSummaryInfo getSellerProductSummary(Long sellerId);
}

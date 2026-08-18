package com.example.secdsp.modules.ai.tool;

import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductAiTool {

    private static final int MAX_RESULTS = 10;

    private final ProductService productService;

    public List<ProductResponse> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return productService
            .getProducts(
                keyword.trim(),
                null,
                null,
                null,
                PageRequest.of(0, MAX_RESULTS)
            )
            .getContent();
    }

    public ProductDetailResponse getProductDetail(Long productId) {
        return productService.getProductById(productId);
    }
}
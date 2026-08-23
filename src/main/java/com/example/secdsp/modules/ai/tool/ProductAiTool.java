package com.example.secdsp.modules.ai.tool;

import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductAiTool {

    private static final int FETCH_SIZE = 50;
    private static final int MAX_RESULTS = 10;

    private final ProductService productService;

    public List<ProductResponse> searchProducts(String keyword) {
        return searchProducts(keyword, null, null);
    }

    /**
     * Search catalog by optional keyword and/or price band.
     * Budget-only queries (blank keyword + maxPrice) return cheapest items under the cap.
     */
    public List<ProductResponse> searchProducts(
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasPrice = minPrice != null || maxPrice != null;
        if (!hasKeyword && !hasPrice) {
            return List.of();
        }

        String sort = hasPrice ? "price-asc" : null;
        String kw = hasKeyword ? keyword.trim() : null;

        List<ProductResponse> raw = productService
            .getProducts(kw, null, null, sort, PageRequest.of(0, FETCH_SIZE))
            .getContent();

        return raw.stream()
            .filter(p -> withinPrice(p, minPrice, maxPrice))
            .limit(MAX_RESULTS)
            .toList();
    }

    private static boolean withinPrice(
        ProductResponse product,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {
        if (product == null || product.getPrice() == null) {
            return false;
        }
        BigDecimal price = product.getPrice();
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
    }

    public ProductDetailResponse getProductDetail(Long productId) {
        return productService.getProductById(productId);
    }
}

package com.example.secdsp.modules.ai.tool;

import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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
        Long sellerScope = shopSellerIdOrNull();

        List<ProductResponse> results = productService
            .getProducts(kw, null, sellerScope, sort, PageRequest.of(0, FETCH_SIZE))
            .getContent()
            .stream()
            .filter(p -> withinPrice(p, minPrice, maxPrice))
            .limit(MAX_RESULTS)
            .toList();

        // If specific keyword gave no results, try extracting primary shopping terms
        if (results.isEmpty() && hasKeyword) {
            String fallbackKeyword = extractFallbackDomainKeyword(kw);
            if (StringUtils.hasText(fallbackKeyword) && !fallbackKeyword.equalsIgnoreCase(kw)) {
                results = productService
                    .getProducts(fallbackKeyword, null, sellerScope, sort, PageRequest.of(0, FETCH_SIZE))
                    .getContent()
                    .stream()
                    .filter(p -> withinPrice(p, minPrice, maxPrice))
                    .limit(MAX_RESULTS)
                    .toList();
            }
        }

        return results;
    }

    /** Seller JWT → only this shop's catalog. Manager/admin/customer keep marketplace search. */
    private static Long shopSellerIdOrNull() {
        if (SecurityUtils.hasRole(UserRole.MANAGER) || SecurityUtils.hasRole(UserRole.ADMIN)) {
            return null;
        }
        if (SecurityUtils.hasRole(UserRole.SELLER)) {
            return SecurityUtils.getCurrentUserId();
        }
        return null;
    }

    private static String extractFallbackDomainKeyword(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("tai nghe") || lower.contains("headphone") || lower.contains("earbuds") || lower.contains("chống ồn") || lower.contains("chong on") || lower.contains("airpods")) {
            return "tai nghe";
        }
        if (lower.contains("bàn phím") || lower.contains("ban phim") || lower.contains("keyboard") || lower.contains("keypro")) {
            return "bàn phím";
        }
        if (lower.contains("nồi chiên") || lower.contains("noi chien") || lower.contains("air fryer") || lower.contains("chiên không dầu")) {
            return "nồi chiên";
        }
        if (lower.contains("giày") || lower.contains("giay") || lower.contains("chạy bộ") || lower.contains("chay bo") || lower.contains("sneaker") || lower.contains("marathon")) {
            return "giày";
        }
        if (lower.contains("chuột") || lower.contains("chuot") || lower.contains("mouse")) {
            return "chuột";
        }
        if (lower.contains("laptop") || lower.contains("macbook") || lower.contains("máy tính")) {
            return "laptop";
        }
        if (lower.contains("điện thoại") || lower.contains("dien thoai") || lower.contains("phone") || lower.contains("iphone") || lower.contains("galaxy") || lower.contains("pixel")) {
            return "điện thoại";
        }
        if (lower.contains("váy") || lower.contains("đầm") || lower.contains("áo") || lower.contains("quần") || lower.contains("hoodie") || lower.contains("blazer")) {
            return "áo";
        }
        if (lower.contains("serum") || lower.contains("son") || lower.contains("cleanser") || lower.contains("mỹ phẩm") || lower.contains("kem")) {
            return "serum";
        }
        if (lower.contains("sofa") || lower.contains("bàn") || lower.contains("đèn") || lower.contains("đồng hồ")) {
            return "bàn";
        }
        return null;
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
        ProductDetailResponse detail = productService.getProductById(productId);
        Long scope = shopSellerIdOrNull();
        if (scope != null && detail != null && detail.getSellerId() != null && !scope.equals(detail.getSellerId())) {
            throw new IllegalArgumentException("Product is not in this shop");
        }
        return detail;
    }
}

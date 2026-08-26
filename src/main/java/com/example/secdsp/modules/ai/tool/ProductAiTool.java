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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ProductAiTool {

    private static final int FETCH_SIZE = 50;
    private static final int MAX_RESULTS = 5;

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

        if (maxPrice == null && StringUtils.hasText(kw)) {
            maxPrice = extractMaxPriceFromText(kw);
            hasPrice = minPrice != null || maxPrice != null;
            if (hasPrice) {
                sort = "price-asc";
            }
        }
        if (StringUtils.hasText(kw) && maxPrice != null) {
            String stripped = stripBudgetTokens(kw);
            kw = StringUtils.hasText(stripped) ? stripped : extractFallbackDomainKeyword(kw);
            hasKeyword = StringUtils.hasText(kw);
        }

        List<ProductResponse> results = productService
            .getProducts(kw, null, sellerScope, sort, PageRequest.of(0, FETCH_SIZE))
            .getContent()
            .stream()
            .filter(p -> withinPrice(p, minPrice, maxPrice))
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
                    .toList();
            }
        }

        return keepCoherentResults(results, kw);
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

    /** Drop the odd-category tail so chat cards stay on one product group. */
    private static List<ProductResponse> keepCoherentResults(
        List<ProductResponse> results,
        String keyword
    ) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        List<ProductResponse> pool = results;
        String domain = StringUtils.hasText(keyword) ? extractFallbackDomainKeyword(keyword) : null;
        if (StringUtils.hasText(domain)) {
            List<ProductResponse> domainHits = results.stream()
                .filter(p -> matchesDomain(p, domain))
                .toList();
            if (!domainHits.isEmpty()) {
                pool = domainHits;
            }
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        int head = Math.min(5, pool.size());
        for (int i = 0; i < head; i++) {
            String cat = categoryKey(pool.get(i));
            if (cat.isEmpty()) {
                continue;
            }
            counts.merge(cat, 1L, Long::sum);
        }
        String dominant = "";
        long max = 0;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                dominant = e.getKey();
            }
        }
        if (!dominant.isEmpty() && max >= 2) {
            final String keep = dominant;
            List<ProductResponse> same = pool.stream()
                .filter(p -> keep.equals(categoryKey(p)))
                .toList();
            if (!same.isEmpty()) {
                pool = same;
            }
        }
        return pool.stream().limit(MAX_RESULTS).toList();
    }

    private static String categoryKey(ProductResponse product) {
        if (product == null || !StringUtils.hasText(product.getCategoryName())) {
            return "";
        }
        return product.getCategoryName().trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesDomain(ProductResponse product, String domain) {
        String hay = haystack(product);
        String d = domain.toLowerCase(Locale.ROOT);
        if (d.contains("tai nghe")) {
            return hay.contains("tai nghe") || hay.contains("headphone") || hay.contains("earbuds")
                || hay.contains("airpods") || hay.contains("headset");
        }
        if (d.contains("điện thoại") || d.contains("dien thoai")) {
            return hay.contains("điện thoại") || hay.contains("dien thoai") || hay.contains("iphone")
                || hay.contains("smartphone") || hay.contains("galaxy") || hay.contains("phone");
        }
        if (d.contains("laptop")) {
            return hay.contains("laptop") || hay.contains("macbook") || hay.contains("notebook");
        }
        if (d.contains("bàn phím") || d.contains("ban phim")) {
            return hay.contains("bàn phím") || hay.contains("ban phim") || hay.contains("keyboard")
                || hay.contains("keypro");
        }
        if (d.contains("giày") || d.contains("giay")) {
            return hay.contains("giày") || hay.contains("giay") || hay.contains("sneaker")
                || hay.contains("shoe");
        }
        if (d.equals("áo") || d.equals("ao")) {
            return hay.contains("áo") || hay.contains("shirt") || hay.contains("hoodie")
                || hay.contains("thời trang") || hay.contains("thoi trang");
        }
        return hay.contains(d);
    }

    private static String haystack(ProductResponse product) {
        String name = product.getName() == null ? "" : product.getName();
        String slug = product.getSlug() == null ? "" : product.getSlug().replace('-', ' ');
        String cat = product.getCategoryName() == null ? "" : product.getCategoryName();
        return (name + " " + slug + " " + cat).toLowerCase(Locale.ROOT);
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

    private static final Pattern UNDER_BUDGET = Pattern.compile(
        "(?i)(?:dưới|duoi|under|tối\\s*đa|toi\\s*da|không\\s*quá|khong\\s*qua|tầm|tam|khoảng|khoang)"
            + "\\s*(\\d+[.,]?\\d*)\\s*(triệu|trieu|tr|m|k|nghìn|nghin)?"
    );

    public static BigDecimal parseMaxPrice(String text) {
        return extractMaxPriceFromText(text);
    }

    private static BigDecimal extractMaxPriceFromText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher m = UNDER_BUDGET.matcher(text);
        if (!m.find()) {
            return null;
        }
        return toVnd(m.group(1), m.group(2));
    }

    private static String stripBudgetTokens(String text) {
        String k = text
            .replaceAll("(?i)(dưới|duoi|under|tối\\s*đa|toi\\s*da|không\\s*quá|khong\\s*qua|"
                + "tầm|tam|khoảng|khoang|ngân\\s*sách|ngan\\s*sach)\\s*\\d+[.,]?\\d*\\s*"
                + "(triệu|trieu|tr|m|k|nghìn|nghin)?", " ")
            .replaceAll("(?i)\\d+[.,]?\\d*\\s*(triệu|trieu|tr)\\b", " ")
            .replaceAll("(?i)(có|co)\\s+(không|khong|ko)\\s*\\??$", "")
            .replaceAll("[?!.]+$", "")
            .replaceAll("\\s+", " ")
            .trim();
        return k.length() < 2 ? "" : k;
    }

    private static BigDecimal toVnd(String amountRaw, String unitRaw) {
        if (amountRaw == null) {
            return null;
        }
        double amount = Double.parseDouble(amountRaw.replace(',', '.'));
        String unit = unitRaw == null ? "" : unitRaw.toLowerCase(Locale.ROOT);
        double multiplier = 1;
        if (unit.startsWith("tr") || unit.equals("m")) {
            multiplier = 1_000_000;
        } else if (unit.startsWith("k") || unit.startsWith("ngh")) {
            multiplier = 1_000;
        } else if (amount < 1000) {
            multiplier = 1_000_000;
        }
        return BigDecimal.valueOf(Math.round(amount * multiplier));
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

package com.example.secdsp.modules.ai.service;

import com.example.secdsp.modules.ai.tool.ProductAiTool;
import com.example.secdsp.modules.ai.tool.VoucherAiTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared catalog/voucher grounding for OpenRouter + DeepSeek (same facts for all LLM fallbacks).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcommercePlatformFactsService {

    private static final Pattern VOUCHER_HINT = Pattern.compile(
        "(?i)voucher|mã\\s*giảm|magiam|coupon|khuyến\\s*mãi|khuyen\\s*mai|giảm\\s*giá|giam\\s*gia|sedsp\\d*"
    );
    private static final Pattern PRODUCT_HINT = Pattern.compile(
        "(?i)sản\\s*phẩm|san\\s*pham|mặt\\s*hàng|mat\\s*hang|giá|gia\\s|mua|tìm|tim\\s|"
            + "laptop|điện\\s*thoại|dien\\s*thoai|phone|tai\\s*nghe|chuột|chuot|bàn\\s*phím|"
            + "ban\\s*phim|máy|may\\s|sku|còn\\s*hàng|con\\s*hang|dưới|duoi|triệu|trieu|rẻ|re |"
            + "danh\\s*mục|danh\\s*muc|gia\\s*dụng|gia\\s*dung|nội\\s*thất|noi\\s*that|phụ\\s*kiện|phu\\s*kien"
    );
    private static final Pattern UNDER_BUDGET = Pattern.compile(
        "(?i)(?:dưới|duoi|under|tối\\s*đa|toi\\s*da|không\\s*quá|khong\\s*qua|tầm|tam|khoảng|khoang)"
            + "\\s*(\\d+[.,]?\\d*)\\s*(triệu|trieu|tr|m|k|nghìn|nghin)?"
    );
    private static final Pattern OVER_BUDGET = Pattern.compile(
        "(?i)(?:trên|tren|from|từ|tu|ít\\s*nhất|it\\s*nhat)"
            + "\\s*(\\d+[.,]?\\d*)\\s*(triệu|trieu|tr|m|k|nghìn|nghin)?"
    );

    private final ProductAiTool productAiTool;
    private final VoucherAiTool voucherAiTool;
    private final ObjectMapper objectMapper;

    public String buildPlatformFacts(String lastUser) {
        if (!StringUtils.hasText(lastUser)) {
            return "";
        }
        StringBuilder facts = new StringBuilder();
        String lower = lastUser.toLowerCase(Locale.ROOT);

        try {
            if (VOUCHER_HINT.matcher(lastUser).find() || lower.contains("mã")) {
                var vouchers = voucherAiTool.listPublicVouchers(null);
                facts.append("public_vouchers=")
                    .append(objectMapper.writeValueAsString(vouchers))
                    .append('\n');
            }
        } catch (Exception e) {
            log.warn("Platform facts voucher grounding failed: {}", e.getMessage());
            facts.append("public_vouchers_error=unavailable\n");
        }

        try {
            BigDecimal minPrice = extractMinPrice(lastUser);
            BigDecimal maxPrice = extractMaxPrice(lastUser);
            boolean budgetQuery = minPrice != null || maxPrice != null;
            boolean productQuery =
                budgetQuery
                    || PRODUCT_HINT.matcher(lastUser).find()
                    || looksLikeProductQuery(lastUser);

            if (productQuery) {
                String keyword = budgetQuery
                    ? stripBudgetTokens(lastUser)
                    : sanitizeKeyword(lastUser);
                var products = productAiTool.searchProducts(keyword, minPrice, maxPrice);
                facts.append("product_search_keyword=").append(keyword == null ? "" : keyword).append('\n');
                if (minPrice != null) {
                    facts.append("minPrice=").append(minPrice).append('\n');
                }
                if (maxPrice != null) {
                    facts.append("maxPrice=").append(maxPrice).append('\n');
                }
                facts.append("products=")
                    .append(objectMapper.writeValueAsString(products))
                    .append('\n');
                facts.append("product_count=").append(products.size()).append('\n');
            }
        } catch (Exception e) {
            log.warn("Platform facts product grounding failed: {}", e.getMessage());
            facts.append("products_error=unavailable\n");
        }

        String text = facts.toString().trim();
        if (text.length() > 6000) {
            return text.substring(0, 6000) + "…";
        }
        return text;
    }

    public boolean looksShoppingRelated(String lastUser) {
        if (!StringUtils.hasText(lastUser)) {
            return false;
        }
        return PRODUCT_HINT.matcher(lastUser).find()
            || VOUCHER_HINT.matcher(lastUser).find()
            || extractMaxPrice(lastUser) != null
            || extractMinPrice(lastUser) != null;
    }

    private static BigDecimal extractMaxPrice(String text) {
        Matcher m = UNDER_BUDGET.matcher(text);
        if (!m.find()) {
            Matcher bare = Pattern.compile(
                "(?i)(\\d+[.,]?\\d*)\\s*(triệu|trieu|tr)\\b"
            ).matcher(text);
            if (bare.find() && Pattern.compile("(?i)dưới|duoi|có\\s*gì|co\\s*gi|ngân\\s*sách|ngan sach")
                .matcher(text).find()) {
                return toVnd(bare.group(1), bare.group(2));
            }
            return null;
        }
        return toVnd(m.group(1), m.group(2));
    }

    private static BigDecimal extractMinPrice(String text) {
        if (!Pattern.compile("(?i)trên|tren|ít\\s*nhất|it\\s*nhat").matcher(text).find()) {
            return null;
        }
        Matcher m = OVER_BUDGET.matcher(text);
        if (!m.find()) {
            return null;
        }
        return toVnd(m.group(1), m.group(2));
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

    private static String stripBudgetTokens(String lastUser) {
        String k = lastUser
            .replaceAll("(?i)(dưới|duoi|under|tối\\s*đa|toi\\s*da|không\\s*quá|khong\\s*qua|trên|tren|"
                + "tầm|tam|khoảng|khoang|ngân\\s*sách|ngan\\s*sach)\\s*\\d+[.,]?\\d*\\s*"
                + "(triệu|trieu|tr|m|k|nghìn|nghin)?", " ")
            .replaceAll("(?i)\\d+[.,]?\\d*\\s*(triệu|trieu|tr)\\b", " ")
            .replaceAll("(?i)^(cho\\s+mình|cho\\s+tôi|toi|mình|minh|bạn|ban|có\\s+gì|co\\s+gi)\\s+", "")
            .replaceAll("(?i)(có|co)\\s+(không|khong|ko)\\s*\\??$", "")
            .replaceAll("[?!.]+$", "")
            .replaceAll("\\s+", " ")
            .trim();
        if (k.length() > 80) {
            k = k.substring(0, 80).trim();
        }
        if (k.length() < 2
            || k.matches("(?i)(sp|sản phẩm|san pham|mặt hàng|mat hang|gì|gi)")) {
            return "";
        }
        return k;
    }

    private static boolean looksLikeProductQuery(String text) {
        String t = text.trim();
        if (t.length() < 2 || t.length() > 120) {
            return false;
        }
        return !t.contains("?") || t.split("\\s+").length <= 8;
    }

    private static String sanitizeKeyword(String lastUser) {
        String k = lastUser.trim()
            .replaceAll("(?i)^(cho\\s+mình|cho\\s+tôi|toi|mình|minh|bạn|ban)\\s+", "")
            .replaceAll("(?i)(có|co)\\s+(không|khong|ko)\\s*\\??$", "")
            .replaceAll("[?!.]+$", "")
            .trim();
        if (k.length() > 80) {
            k = k.substring(0, 80).trim();
        }
        return k;
    }
}

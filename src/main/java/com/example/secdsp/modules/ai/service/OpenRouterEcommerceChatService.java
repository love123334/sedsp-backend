package com.example.secdsp.modules.ai.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.example.secdsp.modules.ai.tool.ProductAiTool;
import com.example.secdsp.modules.ai.tool.VoucherAiTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * OpenRouter (or other OpenAI-compatible) chat with the same assistant rules as Gemini,
 * plus light catalog/voucher grounding so answers stay factual when Gemini is down.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterEcommerceChatService {

    private static final Pattern VOUCHER_HINT = Pattern.compile(
        "(?i)voucher|mã\\s*giảm|magiam|coupon|khuyến\\s*mãi|khuyen\\s*mai|giảm\\s*giá|giam\\s*gia|sedsp\\d*"
    );
    private static final Pattern PRODUCT_HINT = Pattern.compile(
        "(?i)sản\\s*phẩm|san\\s*pham|mặt\\s*hàng|mat\\s*hang|giá|gia\\s|mua|tìm|tim\\s|"
            + "laptop|điện\\s*thoại|dien\\s*thoai|phone|tai\\s*nghe|chuột|chuot|bàn\\s*phím|"
            + "ban\\s*phim|máy|may\\s|sku|còn\\s*hàng|con\\s*hang"
    );

    private final HuggingFaceChatService huggingFaceChatService;
    private final ProductAiTool productAiTool;
    private final VoucherAiTool voucherAiTool;
    private final ObjectMapper objectMapper;

    public boolean isConfigured() {
        return huggingFaceChatService.isConfigured();
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (!isConfigured()) {
            throw new BusinessException(
                "OpenRouter chưa cấu hình (AI_ENABLED + OPENROUTER_API_KEY)."
            );
        }

        String lastUser = lastUserContent(request);
        String facts = buildPlatformFacts(lastUser);

        AiChatRequest bridged = new AiChatRequest();
        List<AiChatRequest.ChatTurn> turns = new ArrayList<>();

        AiChatRequest.ChatTurn system = new AiChatRequest.ChatTurn();
        system.setRole("system");
        system.setContent(
            AiChatPrompts.ECOMMERCE_SYSTEM
                + (facts.isBlank() ? "" : "\n\nPLATFORM_FACTS (source of truth):\n" + facts)
        );
        turns.add(system);

        if (request.getMessages() != null) {
            int start = Math.max(0, request.getMessages().size() - 12);
            for (int i = start; i < request.getMessages().size(); i++) {
                AiChatRequest.ChatTurn src = request.getMessages().get(i);
                if (src == null || "system".equalsIgnoreCase(src.getRole())) {
                    continue;
                }
                AiChatRequest.ChatTurn copy = new AiChatRequest.ChatTurn();
                copy.setRole(src.getRole());
                copy.setContent(src.getContent());
                turns.add(copy);
            }
        }
        bridged.setMessages(turns);

        log.info("OpenRouter ecommerce chat (groundingChars={})", facts.length());
        return huggingFaceChatService.chat(bridged);
    }

    private String buildPlatformFacts(String lastUser) {
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
            log.warn("OpenRouter voucher grounding failed: {}", e.getMessage());
            facts.append("public_vouchers_error=unavailable\n");
        }

        try {
            if (PRODUCT_HINT.matcher(lastUser).find() || looksLikeProductQuery(lastUser)) {
                String keyword = sanitizeKeyword(lastUser);
                if (StringUtils.hasText(keyword)) {
                    var products = productAiTool.searchProducts(keyword);
                    facts.append("product_search_keyword=").append(keyword).append('\n');
                    facts.append("products=")
                        .append(objectMapper.writeValueAsString(products))
                        .append('\n');
                }
            }
        } catch (Exception e) {
            log.warn("OpenRouter product grounding failed: {}", e.getMessage());
            facts.append("products_error=unavailable\n");
        }

        String text = facts.toString().trim();
        if (text.length() > 6000) {
            return text.substring(0, 6000) + "…";
        }
        return text;
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

    private static String lastUserContent(AiChatRequest request) {
        if (request.getMessages() == null) {
            return "";
        }
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            AiChatRequest.ChatTurn turn = request.getMessages().get(i);
            if (turn != null && "user".equalsIgnoreCase(turn.getRole())) {
                return turn.getContent() == null ? "" : turn.getContent();
            }
        }
        return "";
    }
}

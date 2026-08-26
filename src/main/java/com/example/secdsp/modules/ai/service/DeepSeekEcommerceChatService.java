package com.example.secdsp.modules.ai.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.config.DeepSeekProperties;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * DeepSeek ecommerce chat + optional polish of Gemini drafts using the same PLATFORM_FACTS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekEcommerceChatService {

    private static final Pattern WEAK_REPLY = Pattern.compile(
        "(?i)(không\\s*có\\s*(thông\\s*tin|sản\\s*phẩm|dữ\\s*liệu)|"
            + "chua\\s*(tim|co)|không\\s*tìm\\s*thấy|xin\\s*lỗi.*không\\s*thể|"
            + "theo\\s*thông\\s*tin\\s*hiện\\s*tại.*không|"
            + "i\\s*don'?t\\s*have|unable\\s*to\\s*help|as\\s*an\\s*ai)"
    );

    private final DeepSeekChatService deepSeekChatService;
    private final DeepSeekProperties deepSeekProperties;
    private final EcommercePlatformFactsService platformFactsService;

    public boolean isConfigured() {
        return deepSeekChatService.isConfigured();
    }

    public boolean isRefineEnabled() {
        return isConfigured() && deepSeekProperties.isRefineGemini();
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (!isConfigured()) {
            throw new BusinessException("DeepSeek chưa cấu hình (DEEPSEEK_API_KEY).");
        }
        String lastUser = lastUserContent(request);
        String facts = platformFactsService.buildPlatformFacts(lastUser);
        AiChatRequest bridged = bridgeWithFacts(request, facts, null);
        log.info("DeepSeek ecommerce chat (groundingChars={})", facts.length());
        AiChatResponse response = deepSeekChatService.chat(bridged);
        return AiChatResponse.builder()
            .content(response.getContent())
            .provider("deepseek")
            .model(response.getModel())
            .fallback(true)
            .build();
    }

    /**
     * Gemini draft + catalog facts → DeepSeek rewrite for sharper accuracy.
     * Returns null when refine is skipped or fails.
     */
    public AiChatResponse refineGeminiDraft(AiChatRequest request, String geminiDraft) {
        if (!isRefineEnabled() || !StringUtils.hasText(geminiDraft)) {
            return null;
        }
        String lastUser = lastUserContent(request);
        String facts = platformFactsService.buildPlatformFacts(lastUser);
        boolean shopping = platformFactsService.looksShoppingRelated(lastUser);
        boolean weak = looksWeak(geminiDraft);
        boolean hasFacts = StringUtils.hasText(facts);
        boolean draftHasPrice = geminiDraft.matches("(?s).*\\d[\\d.,]*\\s*(đ|₫|VND|triệu|tr|k\\b).*")
            || geminiDraft.matches("(?s).*\\d{1,3}(?:[.,]\\d{3})+.*");
        boolean factsHaveProducts = facts.contains("product_count=")
            && !facts.contains("product_count=0");

        // Skip polish for tiny greetings / no catalog work
        if (!shopping && !weak && !hasFacts) {
            return null;
        }
        if (!weak && !hasFacts && lastUser.length() < 24) {
            return null;
        }
        // Strong Gemini answer that already cites prices → keep (avoid extra latency)
        if (!weak && shopping && draftHasPrice && !(factsHaveProducts && WEAK_REPLY.matcher(geminiDraft).find())) {
            return null;
        }
        // Need something useful to refine with
        if (!weak && !hasFacts && !shopping) {
            return null;
        }

        try {
            AiChatRequest bridged = bridgeWithFacts(request, facts, geminiDraft);
            log.info(
                "DeepSeek refine Gemini draft (factsChars={}, weak={}, shopping={})",
                facts.length(),
                weak,
                shopping
            );
            AiChatResponse refined = deepSeekChatService.chat(bridged);
            String content = refined.getContent();
            if (!StringUtils.hasText(content) || content.length() < 12) {
                return null;
            }
            if (looksLikePromptEcho(content)) {
                log.warn("DeepSeek refine echoed internal prompt; keeping Gemini draft");
                return null;
            }
            return AiChatResponse.builder()
                .content(content)
                .provider("gemini+deepseek")
                .model(refined.getModel())
                .fallback(false)
                .build();
        } catch (Exception e) {
            log.warn("DeepSeek refine failed, keeping Gemini: {}", e.getMessage());
            return null;
        }
    }

    private AiChatRequest bridgeWithFacts(
        AiChatRequest request,
        String facts,
        String geminiDraft
    ) {
        AiChatRequest bridged = new AiChatRequest();
        List<AiChatRequest.ChatTurn> turns = new ArrayList<>();

        AiChatRequest.ChatTurn system = new AiChatRequest.ChatTurn();
        system.setRole("system");
        StringBuilder sys = new StringBuilder(AiChatPrompts.ECOMMERCE_SYSTEM);
        if (StringUtils.hasText(facts)) {
            sys.append("\n\nPLATFORM_FACTS (source of truth — do not invent SKUs/prices):\n")
                .append(facts);
        }
        if (StringUtils.hasText(geminiDraft)) {
            sys.append(
                """

                MULTI_PROVIDER_RULES:
                - You refine a draft from Gemini using PLATFORM_FACTS.
                - Keep Vietnamese with full diacritics; sound like a natural shopping advisor.
                - Prefer PLATFORM_FACTS over the draft when they conflict.
                - If PLATFORM_FACTS has maxPrice, drop any product above that cap — never recommend it.
                - Give 2–4 reasons to pick (budget fit, rating/sold, a real spec). Not a one-liner.
                - Never narrate the UI ("bên dưới", "bấm card", "danh sách sản phẩm", "mình tìm được N…").
                - Do not mention Gemini, DeepSeek, OpenRouter, or that you are refining.
                """.stripIndent()
            );
        }
        system.setContent(sys.toString());
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

        if (StringUtils.hasText(geminiDraft)) {
            AiChatRequest.ChatTurn polish = new AiChatRequest.ChatTurn();
            polish.setRole("user");
            polish.setContent(
                "Bản nháp trợ lý (cần chỉnh cho đúng PLATFORM_FACTS và rõ ràng hơn):\n"
                    + geminiDraft.trim()
                    + "\n\nHãy viết lại câu trả lời cuối cùng cho khách."
            );
            turns.add(polish);
        }

        bridged.setMessages(turns);
        return bridged;
    }

    private static boolean looksWeak(String content) {
        if (!StringUtils.hasText(content)) {
            return true;
        }
        String c = content.trim();
        if (c.length() < 40) {
            return true;
        }
        return WEAK_REPLY.matcher(c).find()
            || c.toLowerCase(Locale.ROOT).contains("không thể tạo câu trả lời");
    }

    private static boolean looksLikePromptEcho(String content) {
        String n = content.toLowerCase(Locale.ROOT);
        return n.contains("platform_facts")
            || n.contains("bản nháp trợ lý")
            || n.contains("hãy viết lại câu trả lời cuối cùng")
            || n.contains("you are sedsp's intelligent")
            || n.contains("multi_provider_rules")
            || n.contains("[english gloss")
            || n.contains("[context sản phẩm");
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

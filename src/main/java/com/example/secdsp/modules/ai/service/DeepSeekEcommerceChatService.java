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
        boolean grounded = lastUser.contains("[CONTEXT SẢN PHẨM/SHOP")
            || lastUser.contains("PLATFORM_FACTS");
        boolean shopping = grounded || platformFactsService.looksShoppingRelated(lastUser);
        boolean weak = looksWeak(geminiDraft);

        // Greetings / tiny chit-chat: keep Gemini, skip extra round
        if (!shopping && !weak && lastUser.length() < 40) {
            return null;
        }

        // FE already sent catalog facts in the user turn — don't spend the 15s budget on another DB search
        String facts = grounded ? "" : platformFactsService.buildPlatformFacts(lastUser);

        try {
            AiChatRequest bridged = bridgeWithFacts(request, facts, geminiDraft);
            log.info(
                "DeepSeek refine Gemini draft (factsChars={}, grounded={}, shopping={})",
                facts.length(),
                grounded,
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
        StringBuilder sys = new StringBuilder(
            StringUtils.hasText(geminiDraft) ? AiChatPrompts.POLISH_SYSTEM : AiChatPrompts.ECOMMERCE_SYSTEM
        );
        if (StringUtils.hasText(facts)) {
            sys.append("\n\nPLATFORM_FACTS (source of truth — do not invent SKUs/prices):\n")
                .append(facts);
        }
        if (StringUtils.hasText(geminiDraft)) {
            sys.append(
                """

                MULTI_PROVIDER_RULES:
                - Rewrite the Gemini draft into a natural advisor reply.
                - Prefer PLATFORM_FACTS / CONTEXT over the draft when they conflict.
                - If maxPrice is set, drop any product above that cap.
                - If the shopper asked for điện thoại, drop tablets (Galaxy Tab, iPad, máy tính bảng).
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

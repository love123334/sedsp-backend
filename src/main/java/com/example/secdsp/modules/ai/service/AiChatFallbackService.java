package com.example.secdsp.modules.ai.service;

import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.google.genai.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Used when Gemini {@link Client} bean is absent.
 * Prefers DeepSeek, then OpenRouter. When Gemini is present,
 * {@link AiChatServiceImpl} handles chat (+ multi-provider fallback/refine).
 */
@Slf4j
@Service
@ConditionalOnMissingBean(Client.class)
@RequiredArgsConstructor
public class AiChatFallbackService implements AiChatService {

    private final DeepSeekEcommerceChatService deepSeekEcommerceChatService;
    private final OpenRouterEcommerceChatService openRouterEcommerceChatService;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (deepSeekEcommerceChatService.isConfigured()) {
            try {
                log.info("Gemini unavailable — answering via DeepSeek");
                return deepSeekEcommerceChatService.chat(request);
            } catch (Exception e) {
                log.warn("DeepSeek failed without Gemini: {}", e.getMessage());
            }
        }
        if (openRouterEcommerceChatService.isConfigured()) {
            log.info("Gemini unavailable — answering via OpenRouter");
            return openRouterEcommerceChatService.chat(request);
        }
        log.warn("Chatbot requested but Gemini / DeepSeek / OpenRouter not configured");
        return AiChatResponse.builder()
            .content(
                "Chatbot chưa được cấu hình trên server "
                    + "(thiếu GEMINI_API_KEY / DEEPSEEK_API_KEY / OPENROUTER_API_KEY). "
                    + "Bạn vẫn có thể hỏi về sản phẩm, voucher và DSS bằng trợ lý trên trang web."
            )
            .provider("none")
            .model("")
            .fallback(true)
            .build();
    }
}

package com.example.secdsp.modules.ai.service;

import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.google.genai.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Used when Gemini {@link Client} bean is absent. Prefers OpenRouter so chat still works.
 * When Gemini is present, {@link AiChatServiceImpl} handles chat (+ OpenRouter on quota/errors).
 */
@Slf4j
@Service
@ConditionalOnMissingBean(Client.class)
@RequiredArgsConstructor
public class AiChatFallbackService implements AiChatService {

    private final OpenRouterEcommerceChatService openRouterEcommerceChatService;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (openRouterEcommerceChatService.isConfigured()) {
            log.info("Gemini unavailable — answering via OpenRouter");
            return openRouterEcommerceChatService.chat(request);
        }
        log.warn("Chatbot requested but neither Gemini nor OpenRouter is configured");
        return AiChatResponse.builder()
            .content(
                "Chatbot chưa được cấu hình trên server (thiếu GEMINI_API_KEY hoặc OPENROUTER_API_KEY). "
                    + "Bạn vẫn có thể hỏi về sản phẩm, voucher và DSS bằng trợ lý trên trang web."
            )
            .provider("none")
            .model("")
            .fallback(true)
            .build();
    }
}

package com.example.secdsp.modules.ai.service;

import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Always registered so Railway boots without GEMINI_API_KEY.
 * {@code @ConditionalOnMissingBean} on a {@code @Service} that implements the
 * same type is skipped during component scan, which crashed production.
 */
@Slf4j
@Service
public class AiChatFallbackService implements AiChatService {

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        log.warn("Gemini chat requested but GEMINI_API_KEY is not configured");
        return AiChatResponse.builder()
            .content(
                "Chatbot Gemini chưa được cấu hình trên server (thiếu GEMINI_API_KEY). "
                    + "Bạn vẫn có thể hỏi về sản phẩm, voucher và DSS bằng trợ lý trên trang web."
            )
            .provider("none")
            .model("")
            .fallback(true)
            .build();
    }
}

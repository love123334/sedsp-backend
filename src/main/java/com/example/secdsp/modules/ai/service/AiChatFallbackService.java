package com.example.secdsp.modules.ai.service;

import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Keeps Railway bootable when GEMINI_API_KEY is not set.
 * Local catalog chatbot on the frontend still answers product questions.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(AiChatService.class)
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

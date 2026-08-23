package com.example.secdsp.modules.ai.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenRouter (or other OpenAI-compatible via HuggingFaceChatService) with catalog grounding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterEcommerceChatService {

    private final HuggingFaceChatService huggingFaceChatService;
    private final EcommercePlatformFactsService platformFactsService;

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
        String facts = platformFactsService.buildPlatformFacts(lastUser);

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

package com.example.secdsp.modules.ai.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.example.secdsp.config.AiProperties;
import com.example.secdsp.config.DeepSeekProperties;
import com.example.secdsp.modules.ai.service.AiChatRateLimiter;
import com.example.secdsp.modules.ai.service.AiChatService;
import com.example.secdsp.modules.ai.service.HuggingFaceChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private static final Set<String> ALLOWED_ROLES = Set.of("system", "user", "assistant");

    private final AiChatRateLimiter aiChatRateLimiter;
    private final AiChatService aiChatService;
    private final HuggingFaceChatService huggingFaceChatService;
    private final AiProperties aiProperties;
    private final DeepSeekProperties deepSeekProperties;

    @Value("${google.ai.api-key:}")
    private String geminiApiKey;

    @Value("${google.ai.model:gemini-3.5-flash-lite}")
    private String geminiModel;

    @GetMapping("/status")
    public ResponseEntity<BaseResponse<Map<String, Object>>> status() {
        boolean gemini = StringUtils.hasText(geminiApiKey);
        boolean deepSeek = deepSeekProperties.isConfigured();
        boolean openRouter = huggingFaceChatService.isConfigured();
        String primary = gemini
            ? "google-gemini"
            : (deepSeek ? "deepseek" : (openRouter ? huggingFaceChatService.providerName() : "none"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("configured", gemini || deepSeek || openRouter);
        body.put("provider", primary);
        body.put(
            "model",
            gemini
                ? geminiModel
                : (deepSeek ? deepSeekProperties.getModel() : (openRouter ? aiProperties.getModel() : ""))
        );
        body.put("geminiConfigured", gemini);
        body.put("deepSeekConfigured", deepSeek);
        body.put("openRouterConfigured", openRouter);
        body.put("deepSeekRefine", deepSeek && deepSeekProperties.isRefineGemini());
        body.put("routing", buildRoutingLabel(gemini, deepSeek, openRouter));
        return ResponseEntity.ok(BaseResponse.success(body));
    }

    private static String buildRoutingLabel(boolean gemini, boolean deepSeek, boolean openRouter) {
        if (gemini && deepSeek && openRouter) {
            return "gemini-primary-deepseek-fallback-openrouter";
        }
        if (gemini && deepSeek) {
            return "gemini-primary-deepseek-fallback";
        }
        if (gemini && openRouter) {
            return "gemini-primary-openrouter-fallback";
        }
        if (gemini) {
            return "gemini-only";
        }
        if (deepSeek && openRouter) {
            return "deepseek-primary-openrouter-fallback";
        }
        if (deepSeek) {
            return "deepseek-only";
        }
        if (openRouter) {
            return "openrouter-only";
        }
        return "none";
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<AiChatResponse>> chat(
        @Valid @RequestBody AiChatRequest request
    ) {
        aiChatRateLimiter.check(SecurityUtils.getCurrentUserId());
        validateTurns(request);

        return ResponseEntity.ok(
            BaseResponse.success(
                "AI reply",
                aiChatService.chat(request)
            )
        );
    }

    private void validateTurns(AiChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BusinessException("Messages required.", HttpStatus.BAD_REQUEST);
        }
        for (AiChatRequest.ChatTurn turn : request.getMessages()) {
            String role = turn.getRole() == null ? "" : turn.getRole().trim().toLowerCase(Locale.ROOT);
            if (!ALLOWED_ROLES.contains(role)) {
                throw new BusinessException("Invalid chat role: " + turn.getRole(), HttpStatus.BAD_REQUEST);
            }
            turn.setRole(role);
            if (turn.getContent() != null && turn.getContent().length() > 8000) {
                throw new BusinessException("Message too long.", HttpStatus.BAD_REQUEST);
            }
        }
    }
}

package com.example.secdsp.modules.ai.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.example.secdsp.config.AiProperties;
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

    @Value("${google.ai.api-key:}")
    private String geminiApiKey;

    @Value("${google.ai.model:gemini-3.6-flash}")
    private String geminiModel;

    @GetMapping("/status")
    public ResponseEntity<BaseResponse<Map<String, Object>>> status() {
        boolean gemini = StringUtils.hasText(geminiApiKey);
        boolean openRouter = huggingFaceChatService.isConfigured();
        String primary = gemini ? "google-gemini" : (openRouter ? huggingFaceChatService.providerName() : "none");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("configured", gemini || openRouter);
        body.put("provider", primary);
        body.put("model", gemini ? geminiModel : (openRouter ? aiProperties.getModel() : ""));
        body.put("geminiConfigured", gemini);
        body.put("openRouterConfigured", openRouter);
        body.put(
            "routing",
            gemini
                ? (openRouter ? "gemini-primary-openrouter-fallback" : "gemini-only")
                : (openRouter ? "openrouter-only" : "none")
        );
        return ResponseEntity.ok(BaseResponse.success(body));
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

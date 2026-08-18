package com.example.secdsp.modules.ai.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.example.secdsp.modules.ai.service.AiChatRateLimiter;
import com.example.secdsp.modules.ai.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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

    @Value("${google.ai.api-key:}")
    private String geminiApiKey;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Map<String, Object>>> status() {
        boolean configured = StringUtils.hasText(geminiApiKey);
        return ResponseEntity.ok(
            BaseResponse.success(
                Map.of(
                    "configured", configured,
                    "provider", configured ? "google-gemini" : "none",
                    "model", configured ? "gemini-3.6-flash" : ""
                )
            )
        );
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

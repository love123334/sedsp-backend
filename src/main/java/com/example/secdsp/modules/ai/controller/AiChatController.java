package com.example.secdsp.modules.ai.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.example.secdsp.modules.ai.service.AiChatRateLimiter;
import com.example.secdsp.modules.ai.service.HuggingFaceChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private static final Set<String> ALLOWED_ROLES = Set.of("system", "user", "assistant");

    private final HuggingFaceChatService huggingFaceChatService;
    private final AiChatRateLimiter aiChatRateLimiter;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(
            BaseResponse.success(
                Map.of(
                    "configured", huggingFaceChatService.isConfigured(),
                    "provider", huggingFaceChatService.providerName()
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
                huggingFaceChatService.chat(request)
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

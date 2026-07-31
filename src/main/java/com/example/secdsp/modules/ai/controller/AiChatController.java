package com.example.secdsp.modules.ai.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.example.secdsp.modules.ai.service.HuggingFaceChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final HuggingFaceChatService huggingFaceChatService;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(
            ApiResponse.success(
                Map.of(
                    "configured", huggingFaceChatService.isConfigured(),
                    "provider", huggingFaceChatService.providerName()
                )
            )
        );
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
        @Valid @RequestBody AiChatRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                "AI reply",
                huggingFaceChatService.chat(request)
            )
        );
    }
}

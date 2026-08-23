package com.example.secdsp.modules.ai.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.config.DeepSeekProperties;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Low-level OpenAI-compatible client for DeepSeek. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekChatService {

    private final DeepSeekProperties deepSeekProperties;
    private final RestTemplate restTemplate;

    public boolean isConfigured() {
        return deepSeekProperties.isConfigured();
    }

    public String modelName() {
        return deepSeekProperties.getModel();
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (!isConfigured()) {
            throw new BusinessException("DeepSeek chưa cấu hình (DEEPSEEK_API_KEY).");
        }

        String base = deepSeekProperties.getBaseUrl().replaceAll("/$", "");
        // Official API accepts /v1/chat/completions; base may already include /v1
        String url = base.endsWith("/v1")
            ? base + "/chat/completions"
            : base + "/v1/chat/completions";

        List<Map<String, String>> messages = new ArrayList<>();
        List<AiChatRequest.ChatTurn> turns = request.getMessages();
        int start = 0;
        if (turns != null && turns.size() > 16) {
            if ("system".equalsIgnoreCase(turns.get(0).getRole())) {
                Map<String, String> sys = new HashMap<>();
                sys.put("role", "system");
                String sysContent = turns.get(0).getContent();
                if (sysContent != null && sysContent.length() > 7000) {
                    sysContent = sysContent.substring(0, 7000);
                }
                sys.put("content", sysContent);
                messages.add(sys);
                start = Math.max(1, turns.size() - 14);
            } else {
                start = turns.size() - 14;
            }
        }
        if (turns != null) {
            for (int i = start; i < turns.size(); i++) {
                AiChatRequest.ChatTurn turn = turns.get(i);
                Map<String, String> m = new HashMap<>();
                String role = turn.getRole() == null ? "user" : turn.getRole().trim().toLowerCase();
                String content = turn.getContent() == null ? "" : turn.getContent();
                if (content.length() > 8000) {
                    content = content.substring(0, 8000);
                }
                m.put("role", role);
                m.put("content", content);
                messages.add(m);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", deepSeekProperties.getModel());
        body.put("messages", messages);
        body.put("temperature", deepSeekProperties.getTemperature());
        body.put("max_tokens", deepSeekProperties.getMaxTokens());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekProperties.getApiKey().trim());

        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> res = restTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                Map.class
            );

            Map<?, ?> payload = res.getBody();
            if (payload == null) {
                throw new BusinessException("DeepSeek không trả về body.");
            }
            if (payload.get("error") != null) {
                throw new BusinessException("DeepSeek: " + String.valueOf(payload.get("error")));
            }

            Object choicesObj = payload.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                throw new BusinessException("DeepSeek không có choices.");
            }
            Object first = choices.get(0);
            if (!(first instanceof Map<?, ?> choice)) {
                throw new BusinessException("DeepSeek choice không hợp lệ.");
            }
            Object messageObj = choice.get("message");
            if (!(messageObj instanceof Map<?, ?> message)) {
                throw new BusinessException("DeepSeek message không hợp lệ.");
            }
            String content = String.valueOf(message.get("content")).trim();
            if (content.isEmpty() || "null".equals(content)) {
                throw new BusinessException("DeepSeek trả về nội dung rỗng.");
            }

            return AiChatResponse.builder()
                .content(content)
                .provider("deepseek")
                .model(deepSeekProperties.getModel())
                .fallback(false)
                .build();

        } catch (RestClientException e) {
            log.warn("DeepSeek chat failed: {}", e.getMessage());
            throw new BusinessException("Không gọi được DeepSeek: " + e.getMessage());
        }
    }
}

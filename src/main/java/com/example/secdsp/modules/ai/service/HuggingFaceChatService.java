package com.example.secdsp.modules.ai.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.config.AiProperties;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class HuggingFaceChatService {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;

    public boolean isConfigured() {
        return aiProperties.isEnabled()
            && aiProperties.getApiToken() != null
            && !aiProperties.getApiToken().isBlank()
            && !aiProperties.getApiToken().startsWith("YOUR_");
    }

    public String providerName() {
        String base = aiProperties.getBaseUrl() == null ? "" : aiProperties.getBaseUrl().toLowerCase();
        if (base.contains("openrouter")) {
            return "openrouter";
        }
        if (base.contains("huggingface") || base.contains("hf.co")) {
            return "huggingface";
        }
        return "openai-compatible";
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (!isConfigured()) {
            throw new BusinessException(
                "AI chua cau hinh. Dat OPENROUTER_API_KEY (hoac AI_API_TOKEN) + AI_ENABLED=true tren backend."
            );
        }

        String url = aiProperties.getBaseUrl().replaceAll("/$", "") + "/chat/completions";

        List<Map<String, String>> messages = new ArrayList<>();
        for (AiChatRequest.ChatTurn turn : request.getMessages()) {
            Map<String, String> m = new HashMap<>();
            m.put("role", turn.getRole());
            m.put("content", turn.getContent());
            messages.add(m);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("messages", messages);
        body.put("temperature", aiProperties.getTemperature());
        body.put("max_tokens", aiProperties.getMaxTokens());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiToken().trim());
        if ("openrouter".equals(providerName())) {
            headers.set("HTTP-Referer", "https://sedsp.local");
            headers.set("X-Title", "SEDSP Chatbot");
        }

        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> res = restTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                Map.class
            );

            Map<?, ?> payload = res.getBody();
            if (payload == null) {
                throw new BusinessException("AI provider khong tra ve body.");
            }
            if (payload.get("error") != null) {
                throw new BusinessException("AI provider: " + String.valueOf(payload.get("error")));
            }

            Object choicesObj = payload.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                throw new BusinessException("AI provider khong co choices.");
            }
            Object first = choices.get(0);
            if (!(first instanceof Map<?, ?> choice)) {
                throw new BusinessException("AI choice khong hop le.");
            }
            Object messageObj = choice.get("message");
            if (!(messageObj instanceof Map<?, ?> message)) {
                throw new BusinessException("AI message khong hop le.");
            }
            String content = String.valueOf(message.get("content")).trim();
            if (content.isEmpty() || "null".equals(content)) {
                throw new BusinessException("AI tra ve noi dung rong.");
            }

            return AiChatResponse.builder()
                .content(content)
                .provider(providerName())
                .model(aiProperties.getModel())
                .fallback(false)
                .build();

        } catch (RestClientException e) {
            log.warn("AI chat failed: {}", e.getMessage());
            throw new BusinessException("Khong goi duoc AI: " + e.getMessage());
        }
    }

    public String generateInsightPlan(String metricsJson) {
        if (!isConfigured()) {
            return ruleBasedPlan(metricsJson);
        }

        AiChatRequest req = new AiChatRequest();
        List<AiChatRequest.ChatTurn> turns = new ArrayList<>();

        AiChatRequest.ChatTurn system = new AiChatRequest.ChatTurn();
        system.setRole("system");
        system.setContent(
            "Ban la chuyen gia DSS thuong mai dien tu (SEDSP). "
                + "Dua tren so lieu JSON, viet tieng Viet: "
                + "(1) Nhan xet tinh hinh 3-5 cau, "
                + "(2) Ke hoach hanh dong 4-6 buoc cu the, "
                + "(3) Rui ro can theo doi. "
                + "Khong bia so lieu ngoai JSON. Dung markdown ngan gon."
        );
        turns.add(system);

        AiChatRequest.ChatTurn user = new AiChatRequest.ChatTurn();
        user.setRole("user");
        user.setContent("So lieu DSS / Power BI feed:\n" + metricsJson);
        turns.add(user);

        req.setMessages(turns);
        try {
            return chat(req).getContent();
        } catch (Exception e) {
            log.warn("Insight AI fallback: {}", e.getMessage());
            return ruleBasedPlan(metricsJson);
        }
    }

    private String ruleBasedPlan(String metricsJson) {
        return """
            ## Nhan xet
            He thong da tong hop so lieu ban hang / ton kho tu SEDSP (nguon web).
            AI chua bat — dang dung ke hoach mau dua tren du lieu hien co.

            ## Ke hoach de xuat
            1. Kiem tra SKU ton thap va uu tien nhap hang.
            2. Theo doi SP ban chay de dam bao cung ung.
            3. Thu nghiem giam gia 5-10%% tren SP ban cham (what-if).
            4. Dong bo bao cao Power BI hang ngay qua API analytics.
            5. Sau khi cau hinh OPENROUTER_API_KEY, bat lai de sinh nhan xet tu dong.

            ## Rui ro
            Thieu token AI hoac du lieu don hang it co the lam giam do tin cay du bao.

            ---
            Metrics snapshot:
            %s
            """.formatted(metricsJson.length() > 1200 ? metricsJson.substring(0, 1200) + "..." : metricsJson);
    }
}

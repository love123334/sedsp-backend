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

    @org.springframework.beans.factory.annotation.Value("${app.frontend.base-url:https://smartecon.vercel.app}")
    private String frontendBaseUrl;

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
            String referer = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                ? "https://smartecon.vercel.app"
                : frontendBaseUrl.replaceAll("/$", "");
            headers.set("HTTP-Referer", referer);
            headers.set("X-Title", "SEDSP DSS");
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

    public String generateInsightPlan(String metricsBrief) {
        if (!isConfigured()) {
            return sanitizeInsightCommentary(ruleBasedPlan(metricsBrief));
        }

        AiChatRequest req = new AiChatRequest();
        List<AiChatRequest.ChatTurn> turns = new ArrayList<>();

        AiChatRequest.ChatTurn system = new AiChatRequest.ChatTurn();
        system.setRole("system");
        system.setContent(
            """
            Bạn là cố vấn kinh doanh SEDSP cho người bán trên sàn thương mại điện tử Việt Nam.
            Viết tiếng Việt có đầy đủ dấu, giọng chuyên nghiệp, ngắn gọn, dễ hành động.

            BẮT BUỘC đúng 3 mục markdown (không thiếu mục nào):
            ## Nhận xét tình hình
            (3–5 câu: nêu tồn kho, mặt hàng bán chạy bằng tên thương mại, và tín hiệu nhu cầu/giá nếu có)

            ## Kế hoạch hành động
            (5–7 bước đánh số, cụ thể trong 7 ngày tới; nhắc dùng DSS: dự báo nhu cầu, gợi ý giá, what-if giảm giá, tồn kho)

            ## Rủi ro cần theo dõi
            (2–4 gạch đầu dòng; KHÔNG được để trống)

            CẤM:
            - Tiếng Trung / Anh / ngôn ngữ khác xen vào
            - Tiêu đề không dấu (vd: "Nhan xet", "Ke hoach")
            - Endpoint API, đường dẫn /api/..., tên biến kỹ thuật (topProducts, productId, lowStockCount, inventoryMessage, ROP, SKU JSON…)
            - JSON thô, metrics snapshot, hướng dẫn cấu hình token

            Chỉ dùng số liệu trong phần tóm tắt người dùng cung cấp; không bịa thêm doanh thu/số lượng.
            """.stripIndent()
        );
        turns.add(system);

        AiChatRequest.ChatTurn user = new AiChatRequest.ChatTurn();
        user.setRole("user");
        user.setContent(metricsBrief == null || metricsBrief.isBlank()
            ? "Chưa có số liệu chi tiết. Viết kế hoạch kiểm tra DSS chung."
            : metricsBrief);
        turns.add(user);

        req.setMessages(turns);
        try {
            return sanitizeInsightCommentary(chat(req).getContent());
        } catch (Exception e) {
            log.warn("Insight AI fallback: {}", e.getMessage());
            return sanitizeInsightCommentary(ruleBasedPlan(metricsBrief));
        }
    }

    /** Làm sạch / chuẩn hóa commentary trước khi trả UI */
    static String sanitizeInsightCommentary(String raw) {
        if (raw == null || raw.isBlank()) {
            return ruleBasedPlan(null);
        }
        String text = raw.trim();
        // Bỏ đường dẫn API / endpoint
        text = text.replaceAll("(?i)/api/v1/[^\\s)\\]]+", "");
        text = text.replaceAll("(?i)https?://\\S+", "");
        text = text.replaceAll("(?i)\\b(powerbiFeed|topProducts|productId|lowStockCount|inventoryMessage|inventoryOverall|sellerId)\\b", "");
        // Bỏ ký tự CJK (Trung/Nhật/Hàn) lẫn vào
        text = text.replaceAll("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff\\uac00-\\ud7af]+", "");
        // Chuẩn hóa tiêu đề (## hoặc **...**) không dấu → có dấu markdown ##
        text = text.replaceAll("(?im)^(?:#{1,3}\\s*|\\*{1,2}\\s*)Nhan\\s*xet(?:\\s+tinh\\s+hinh)?\\s*\\*{0,2}\\s*$", "## Nhận xét tình hình");
        text = text.replaceAll("(?im)^(?:#{1,3}\\s*|\\*{1,2}\\s*)Ke\\s*hoach(?:\\s+hanh\\s+dong)?\\s*\\*{0,2}\\s*$", "## Kế hoạch hành động");
        text = text.replaceAll("(?im)^(?:#{1,3}\\s*|\\*{1,2}\\s*)Rui\\s*ro(?:\\s+can\\s+theo\\s+doi)?\\s*\\*{0,2}\\s*$", "## Rủi ro cần theo dõi");
        text = text.replaceAll("(?im)^(?:#{1,3}\\s*|\\*{1,2}\\s*)Nhận xét(?:\\s*\\([^)]*\\))?\\s*\\*{0,2}\\s*$", "## Nhận xét tình hình");
        text = text.replaceAll("(?im)^(?:#{1,3}\\s*|\\*{1,2}\\s*)Nhận xét tình hình\\s*\\*{0,2}\\s*$", "## Nhận xét tình hình");
        text = text.replaceAll("(?im)^(?:#{1,3}\\s*|\\*{1,2}\\s*)Kế hoạch(?:\\s+hành động|\\s+đề xuất)?(?:\\s*\\([^)]*\\))?\\s*\\*{0,2}\\s*$", "## Kế hoạch hành động");
        text = text.replaceAll("(?im)^(?:#{1,3}\\s*|\\*{1,2}\\s*)Rủi ro(?:\\s+cần theo dõi)?\\s*\\*{0,2}\\s*$", "## Rủi ro cần theo dõi");
        // Bỏ block metrics còn sót
        text = text.replaceAll("(?is)\\n*-{2,}\\s*\\n*Metrics snapshot:.*$", "");
        text = text.replaceAll("(?is)\\n*Metrics snapshot:.*$", "");
        text = text.replaceAll("[ \\t]{2,}", " ");
        text = text.replaceAll("\\n{3,}", "\n\n").trim();

        boolean hasNhanXet = text.matches("(?is).*##\\s*Nhận xét.*");
        boolean hasKeHoach = text.matches("(?is).*##\\s*Kế hoạch.*");
        boolean hasRuiRo = text.matches("(?is).*##\\s*Rủi ro.*");
        if (!hasNhanXet || !hasKeHoach) {
            return ruleBasedPlan(null);
        }
        if (!hasRuiRo || risksSectionEmpty(text)) {
            text = text.replaceAll("(?is)\\n*##\\s*Rủi ro[^\\n]*\\s*$", "").trim();
            text = text + "\n\n## Rủi ro cần theo dõi\n"
                + "- Tồn kho không theo kịp nhu cầu thực tế nếu chậm nhập hàng.\n"
                + "- Số liệu bán chạy thay đổi nhanh khi có khuyến mãi đột xuất.\n"
                + "- Thiếu theo dõi hàng ngày dễ bỏ lỡ sản phẩm sắp hết.";
        }
        return text.trim();
    }

    private static boolean risksSectionEmpty(String text) {
        int idx = text.toLowerCase().indexOf("## rủi ro");
        if (idx < 0) {
            return true;
        }
        String after = text.substring(idx).replaceFirst("(?is)^##\\s*Rủi ro[^\\n]*\\n*", "").trim();
        // Hết file hoặc chỉ còn heading khác ngay sau
        return after.isEmpty() || after.startsWith("##");
    }

    private static String ruleBasedPlan(String metricsBrief) {
        String lowHint = "";
        String topHint = "";
        if (metricsBrief != null && !metricsBrief.isBlank()) {
            java.util.regex.Matcher low = java.util.regex.Pattern
                .compile("Số mặt hàng cần nhập thêm:\\s*(\\d+)")
                .matcher(metricsBrief);
            if (low.find()) {
                lowHint = low.group(1);
            }
            java.util.regex.Matcher top = java.util.regex.Pattern
                .compile("(?m)^\\s*\\d+\\.\\s+(.+?)\\s+—")
                .matcher(metricsBrief);
            if (top.find()) {
                topHint = top.group(1).trim();
            }
        }

        String situation = "Hệ thống đã tổng hợp tình hình bán hàng và tồn kho hiện có trên SEDSP. "
            + (lowHint.isBlank()
                ? "Bạn nên ưu tiên các mặt hàng bán chạy và kiểm tra sản phẩm sắp hết để tránh gián đoạn bán."
                : ("Hiện có khoảng " + lowHint + " mặt hàng cần bổ sung tồn. "
                    + (topHint.isBlank()
                        ? "Ưu tiên nhập hàng và giữ nguồn cung ổn định."
                        : ("Đặc biệt giữ nguồn cung cho \"" + topHint + "\" đang bán chạy."))));

        return """
            ## Nhận xét tình hình
            %s

            ## Kế hoạch hành động
            1. Mở **Dự báo nhu cầu** cho 2–3 SKU bán chạy và sản phẩm tồn thấp.
            2. Chạy **Gợi ý giá** để kiểm tra biên lợi nhuận trước khi giảm giá.
            3. Dùng **What-if giảm giá** (5–10%%) trên SKU tồn cao để đẩy hàng có kiểm soát.
            4. Rà soát **Khuyến nghị tồn kho** và lên lịch nhập trong tuần này.
            5. Theo dõi đơn bán / doanh số mỗi ngày; ưu tiên giao đơn đang chờ.
            6. Sau 7 ngày, so lại nhu cầu thực tế với dự báo và điều chỉnh kế hoạch.

            ## Rủi ro cần theo dõi
            - Nhập chậm có thể khiến mặt hàng bán chạy bị hết tồn.
            - Dữ liệu đơn hàng ít sẽ làm dự báo nhu cầu kém chính xác hơn.
            - Khuyến mãi đột xuất có thể làm lệch nhu cầu so với kế hoạch ban đầu.
            """.formatted(situation).stripIndent();
    }
}
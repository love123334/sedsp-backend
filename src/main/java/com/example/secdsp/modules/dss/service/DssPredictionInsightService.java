package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.service.HuggingFaceChatService;
import com.example.secdsp.modules.dss.dto.response.DssAiInsightResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * AI phân tích DSS — chỉ dựa trên số liệu nội bộ đã xác minh; không bịa SKU hay doanh số.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DssPredictionInsightService {

    /** Không chặn API dự báo — OpenRouter có thể >15s; fallback local nếu quá hạn. */
    private static final long AI_INSIGHT_TIMEOUT_SECONDS = 5;

    private static final String DISCLAIMER =
        "Phân tích dựa trên dữ liệu bán hàng và tồn kho của shop. "
            + "Không phải báo cáo thị trường thời gian thực từ web.";

    private final HuggingFaceChatService chatService;

    public DssAiInsightResponse generateDemandInsight(String factsBrief) {
        return generate(
            "Nhu cầu & mùa vụ",
            """
            Bạn là chuyên gia DSS cho seller TMĐT Việt Nam.
            Dựa CHỈ trên số liệu trong phần FACTS, viết tiếng Việt có dấu (120–180 từ):
            - 1 đoạn nhận định xu hướng nhu cầu
            - 2–3 gạch đầu dòng: tồn kho / nhập hàng theo dự báo
            - 1 gạch: rủi ro hoặc điều cần theo dõi
            KHÔNG bịa số, KHÔNG endpoint API, KHÔNG tiếng Anh/Trung xen vào.
            """,
            factsBrief,
            ruleBasedDemand(factsBrief)
        );
    }

    public DssAiInsightResponse generatePriceInsight(String factsBrief) {
        return generate(
            "Giá & đàn hồi",
            """
            Bạn là chuyên gia định giá cho seller TMĐT Việt Nam.
            Dựa CHỈ trên FACTS, viết tiếng Việt có dấu (120–180 từ):
            - Nhận xét độ nhạy giá (elasticity) và lần chỉnh giá gần nhất (nếu có)
            - 2–3 gạch: gợi ý hành động giá/khuyến mãi phù hợp mùa
            - 1 gạch: cảnh báo nếu chỉnh giá làm giảm lượt mua
            KHÔNG bịa số, KHÔNG endpoint API.
            """,
            factsBrief,
            ruleBasedPrice(factsBrief)
        );
    }

    private DssAiInsightResponse generate(
        String title,
        String systemPrompt,
        String factsBrief,
        String ruleFallback
    ) {
        if (!chatService.isConfigured()) {
            return DssAiInsightResponse.builder()
                .title(title)
                .summary(ruleFallback)
                .provider("local")
                .fallback(true)
                .disclaimer(DISCLAIMER)
                .build();
        }

        AiChatRequest req = new AiChatRequest();
        List<AiChatRequest.ChatTurn> turns = new ArrayList<>();
        AiChatRequest.ChatTurn system = new AiChatRequest.ChatTurn();
        system.setRole("system");
        system.setContent(systemPrompt);
        turns.add(system);

        AiChatRequest.ChatTurn user = new AiChatRequest.ChatTurn();
        user.setRole("user");
        user.setContent("FACTS:\n" + (factsBrief == null ? "" : factsBrief));
        turns.add(user);
        req.setMessages(turns);

        try {
            String content = CompletableFuture
                .supplyAsync(() -> HuggingFaceChatService.sanitizeInsightCommentary(
                    chatService.chat(req).getContent()
                ))
                .orTimeout(AI_INSIGHT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("DSS prediction AI timeout/error: {}", ex.getMessage());
                    return null;
                })
                .join();
            if (content != null && !content.isBlank()) {
                return DssAiInsightResponse.builder()
                    .title(title)
                    .summary(content)
                    .provider(chatService.providerName())
                    .fallback(false)
                    .disclaimer(DISCLAIMER)
                    .build();
            }
        } catch (Exception e) {
            log.warn("DSS prediction AI fallback: {}", e.getMessage());
        }
        return DssAiInsightResponse.builder()
            .title(title)
            .summary(ruleFallback)
            .provider("local")
            .fallback(true)
            .disclaimer(DISCLAIMER)
            .build();
    }

    static String ruleBasedDemand(String facts) {
        if (facts == null || facts.isBlank()) {
            return "Theo dõi biểu đồ bán hàng và điều chỉnh tồn kho theo nhịp dự báo.";
        }
        return "Dự báo đã tính xu hướng và mùa vụ theo thứ trong tuần. "
            + "So sánh đường dự báo với lịch sử bán — nếu chênh lớn, kiểm tra tồn kho. "
            + facts.lines().findFirst().orElse("");
    }

    static String ruleBasedPrice(String facts) {
        if (facts == null || facts.isBlank()) {
            return "Xem lại lịch sử chỉnh giá: nếu tăng giá làm giảm lượt mua, cân nhắc khuyến mãi có thời hạn thay vì hạ chất lượng.";
        }
        return "Ưu tiên kịch bản lợi nhuận ròng tối đa, đồng thời kiểm tra tác động ±7 ngày quanh mỗi lần đổi giá. "
            + facts.lines().findFirst().orElse("");
    }
}

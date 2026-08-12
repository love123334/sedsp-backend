package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.service.HuggingFaceChatService;
import com.example.secdsp.modules.dss.dto.response.DssAiInsightResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI phân tích DSS — chỉ dựa trên số liệu nội bộ đã xác minh; không bịa SKU hay doanh số.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DssPredictionInsightService {

    private static final String DISCLAIMER =
        "Phân tích kết hợp dữ liệu shop + mùa vụ TMĐT VN (Tết, 11.11, 12.12…). "
            + "Không phải báo cáo thị trường thời gian thực từ web.";

    private final HuggingFaceChatService chatService;

    public DssAiInsightResponse generateDemandInsight(String factsBrief) {
        return generate(
            "Nhu cầu & mùa vụ",
            """
            Bạn là chuyên gia DSS cho seller TMĐT Việt Nam.
            Dựa CHỈ trên số liệu trong phần FACTS, viết tiếng Việt có dấu (120–180 từ):
            - 1 đoạn nhận định xu hướng nhu cầu
            - 2–3 gạch đầu dòng: tác động ngày lễ/khuyến mãi sắp tới (nếu có trong FACTS)
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
            String content = HuggingFaceChatService.sanitizeInsightCommentary(
                chatService.chat(req).getContent()
            );
            return DssAiInsightResponse.builder()
                .title(title)
                .summary(content)
                .provider(chatService.providerName())
                .fallback(false)
                .disclaimer(DISCLAIMER)
                .build();
        } catch (Exception e) {
            log.warn("DSS prediction AI fallback: {}", e.getMessage());
            return DssAiInsightResponse.builder()
                .title(title)
                .summary(ruleFallback)
                .provider("local")
                .fallback(true)
                .disclaimer(DISCLAIMER)
                .build();
        }
    }

    static String ruleBasedDemand(String facts) {
        if (facts == null || facts.isBlank()) {
            return "Theo dõi biểu đồ bán hàng và điều chỉnh tồn kho trước các đợt sale lớn (11.11, 12.12, Tết).";
        }
        return "Dự báo đã tính xu hướng và hệ số ngày lễ VN. "
            + "So sánh dự báo có mùa vụ với trung bình phẳng — nếu chênh lớn, chuẩn bị hàng trước sự kiện. "
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

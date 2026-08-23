package com.example.secdsp.modules.ai.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.ai.dto.AiChatRequest;
import com.example.secdsp.modules.ai.dto.AiChatResponse;
import com.example.secdsp.modules.ai.tool.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Primary
@ConditionalOnBean(Client.class)
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final Client googleAiClient;
    private final ProductAiTool productAiTool;
    private final OrderAiTool orderAiTool;
    private final InventoryAiTool inventoryAiTool;
    private final VoucherAiTool voucherAiTool;
    private final ObjectMapper objectMapper;
    private final DeepSeekEcommerceChatService deepSeekEcommerceChatService;
    private final OpenRouterEcommerceChatService openRouterEcommerceChatService;

    @Value("${google.ai.model}")
    private String model;

    private static final int MAX_HISTORY_MESSAGES = 8;
    private static final List<String> MODEL_FALLBACKS = List.of(
        "gemini-3.6-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-3-flash-preview"
    );

    @Override
    public AiChatResponse chat(AiChatRequest request) {

        long totalStart = System.currentTimeMillis();

        try {
            String conversation = buildConversation(request);
            GenerateContentConfig toolsConfig = buildConfig(true);
            GenerateContentConfig plainConfig = buildConfig(false);

            long firstCallStart = System.currentTimeMillis();
            GenerateContentResponse response;
            try {
                response = generateWithModelFallback(conversation, toolsConfig);
            } catch (Exception toolsFailure) {
                log.warn(
                    "Gemini with tools failed ({}), retrying without tools",
                    rootMessage(toolsFailure)
                );
                response = generateWithModelFallback(conversation, plainConfig);
            }
            long firstCallMs = System.currentTimeMillis() - firstCallStart;
            log.info("AI Gemini #1 completed: {} ms", firstCallMs);

            FunctionCall functionCall = extractFunctionCall(response);
            if (functionCall == null) {
                log.info(
                    "AI completed without tool. Total: {} ms",
                    System.currentTimeMillis() - totalStart
                );
                return finalizeGeminiReply(request, buildResponse(response.text()));
            }

            String functionName = functionCall.name().orElse("");
            Map<String, Object> functionArgs = functionCall.args()
                .map(value -> (Map<String, Object>) value)
                .orElse(Map.of());
            log.info("Gemini requested tool: {} args: {}", functionName, functionArgs);

            long toolStart = System.currentTimeMillis();
            Map<String, Object> toolResult = executeAiTool(functionCall);
            long toolMs = System.currentTimeMillis() - toolStart;
            log.info("AI tool {} completed: {} ms", functionName, toolMs);

            // Preserve Gemini function-call content (incl. thought_signature) and
            // return tool output via fromFunctionResponse — required by Gemini 3.x.
            Map<String, Object> serializableToolResult = objectMapper.convertValue(
                toolResult,
                new TypeReference<Map<String, Object>>() {}
            );
            Content toolResponseContent = Content.builder()
                .role("user")
                .parts(
                    List.of(
                        Part.fromFunctionResponse(functionName, serializableToolResult)
                    )
                )
                .build();

            Content modelResponseContent = response.candidates()
                .orElse(List.of())
                .stream()
                .findFirst()
                .flatMap(Candidate::content)
                .orElseThrow(
                    () -> new IllegalStateException(
                        "Gemini response did not contain model content."
                    )
                );

            List<Content> followUpContents = List.of(
                modelResponseContent,
                toolResponseContent
            );

            long secondCallStart = System.currentTimeMillis();
            GenerateContentResponse finalResponse =
                generateWithModelFallback(followUpContents, toolsConfig);
            long secondCallMs = System.currentTimeMillis() - secondCallStart;
            long totalMs = System.currentTimeMillis() - totalStart;

            log.info("AI Gemini #2 completed: {} ms", secondCallMs);
            log.info(
                "AI chat total completed: {} ms | Gemini #1: {} ms | Tool: {} ms | Gemini #2: {} ms",
                totalMs,
                firstCallMs,
                toolMs,
                secondCallMs
            );

            return finalizeGeminiReply(request, buildResponse(finalResponse.text()));

        } catch (Exception e) {
            log.warn(
                "Gemini chat failed after {} ms ({}), trying DeepSeek → OpenRouter fallback",
                System.currentTimeMillis() - totalStart,
                rootMessage(e)
            );
            AiChatResponse viaFallback = tryMultiProviderFallback(request);
            if (viaFallback != null) {
                return viaFallback;
            }
            if (e instanceof BusinessException be) {
                throw be;
            }
            String detail = rootMessage(e);
            if (isNonRetryableGeminiError(e) && detail.toLowerCase().contains("quota")) {
                throw new BusinessException(
                    "Chatbot Gemini tạm hết hạn mức (quota) và DeepSeek/OpenRouter chưa sẵn sàng. "
                        + "Kiểm tra GEMINI_API_KEY / DEEPSEEK_API_KEY / OPENROUTER_API_KEY.",
                    HttpStatus.TOO_MANY_REQUESTS
                );
            }
            String shortDetail = detail.length() > 220 ? detail.substring(0, 217) + "…" : detail;
            throw new BusinessException(
                "Chatbot tạm lỗi: " + shortDetail,
                HttpStatus.BAD_GATEWAY
            );
        }
    }

    /** Gemini draft → optional DeepSeek polish with shared catalog facts. */
    private AiChatResponse finalizeGeminiReply(AiChatRequest request, AiChatResponse gemini) {
        if (gemini == null || !StringUtils.hasText(gemini.getContent())) {
            return gemini;
        }
        if (deepSeekEcommerceChatService == null || !deepSeekEcommerceChatService.isRefineEnabled()) {
            return gemini;
        }
        AiChatResponse refined = deepSeekEcommerceChatService.refineGeminiDraft(
            request,
            gemini.getContent()
        );
        if (refined != null && StringUtils.hasText(refined.getContent())) {
            log.info("Multi-provider reply: gemini+deepseek");
            return refined;
        }
        return gemini;
    }

    /** Cascade when Gemini is down: DeepSeek (grounded) → OpenRouter (grounded). */
    private AiChatResponse tryMultiProviderFallback(AiChatRequest request) {
        if (deepSeekEcommerceChatService != null && deepSeekEcommerceChatService.isConfigured()) {
            try {
                AiChatResponse response = deepSeekEcommerceChatService.chat(request);
                log.info("DeepSeek chatbot fallback succeeded");
                return response;
            } catch (Exception deepSeekFailure) {
                log.warn("DeepSeek chatbot fallback failed: {}", rootMessage(deepSeekFailure));
            }
        }
        return tryOpenRouterFallback(request);
    }

    private AiChatResponse tryOpenRouterFallback(AiChatRequest request) {
        if (openRouterEcommerceChatService == null
            || !openRouterEcommerceChatService.isConfigured()) {
            return null;
        }
        try {
            AiChatResponse response = openRouterEcommerceChatService.chat(request);
            log.info(
                "OpenRouter chatbot fallback succeeded (provider={})",
                response.getProvider()
            );
            return response;
        } catch (Exception openRouterFailure) {
            log.warn(
                "OpenRouter chatbot fallback failed: {}",
                rootMessage(openRouterFailure)
            );
            return null;
        }
    }

    private GenerateContentConfig buildConfig(boolean withTools) {
        GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
            .systemInstruction(
                Content.fromParts(Part.fromText(AiChatPrompts.ECOMMERCE_SYSTEM))
            );
        if (withTools) {
            builder.tools(
                List.of(
                    Tool.builder()
                        .functionDeclarations(
                            List.of(
                                ProductToolDefinition.searchProducts(),
                                ProductToolDefinition.getProductDetail(),
                                OrderToolDefinition.getMyOrders(),
                                OrderToolDefinition.getOrderDetail(),
                                InventoryToolDefinition.getInventory(),
                                InventoryToolDefinition.getInventorySummary(),
                                InventoryToolDefinition.getLowStockProducts(),
                                VoucherToolDefinition.listPublicVouchers(),
                                VoucherToolDefinition.validateVoucher()
                            )
                        )
                        .build()
                )
            );
        }
        return builder.build();
    }

    private List<String> modelCandidates() {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        if (StringUtils.hasText(model)) {
            models.add(model.trim());
        }
        models.addAll(MODEL_FALLBACKS);
        return new ArrayList<>(models);
    }

    private GenerateContentResponse generateWithModelFallback(
        Object contents,
        GenerateContentConfig config
    ) throws Exception {
        List<String> failures = new ArrayList<>();
        for (String candidate : modelCandidates()) {
            try {
                GenerateContentResponse response = contents instanceof String text
                    ? googleAiClient.models.generateContent(candidate, text, config)
                    : googleAiClient.models.generateContent(
                        candidate,
                        (List<Content>) contents,
                        config
                    );
                if (!candidate.equals(model)) {
                    log.info("Gemini model fallback succeeded with {}", candidate);
                }
                return response;
            } catch (Exception ex) {
                String detail = candidate + "=" + rootMessage(ex);
                failures.add(detail);
                log.warn("Gemini model {} failed: {}", candidate, rootMessage(ex));
                // Shared project quota / auth — trying more models only burns the same limit
                if (isNonRetryableGeminiError(ex)) {
                    break;
                }
            }
        }
        throw new IllegalStateException(
            "All Gemini models failed: " + String.join(" | ", failures)
        );
    }

    private static boolean isNonRetryableGeminiError(Throwable throwable) {
        String message = rootMessage(throwable).toLowerCase();
        return message.contains("429")
            || message.contains("too many requests")
            || message.contains("exceeded your current quota")
            || message.contains("401")
            || message.contains("403")
            || message.contains("api key")
            || message.contains("permission denied");
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

    private String buildConversation(AiChatRequest request) {

        List<AiChatRequest.ChatTurn> messages = request.getMessages();

        int start = Math.max(
            0,
            messages.size() - MAX_HISTORY_MESSAGES
        );

        StringBuilder input = new StringBuilder();

        for (int i = start; i < messages.size(); i++) {

            AiChatRequest.ChatTurn turn = messages.get(i);

            input.append(turn.getRole())
                .append(": ")
                .append(turn.getContent())
                .append("\n");
        }

        return input.toString();
    }

    private FunctionCall extractFunctionCall(
        GenerateContentResponse response
    ) {

        if (response.candidates().isEmpty()) {
            return null;
        }

        Content content =
            response.candidates()
                .get()
                .get(0)
                .content()
                .orElse(null);

        if (content == null || content.parts().isEmpty()) {
            return null;
        }

        for (Part part : content.parts().get()) {
            if (part.functionCall().isPresent()) {
                return part.functionCall().get();
            }
        }

        return null;
    }

    private Map<String, Object> executeAiTool(
        FunctionCall functionCall
    ) {

        String functionName =
            functionCall.name().orElse("");

        Map<String, Object> args =
            functionCall.args()
                .map(value -> (Map<String, Object>) value)
                .orElse(Map.of());

        log.info(
            "Executing AI tool: {} with args: {}",
            functionName,
            args
        );

        return switch (functionName) {

            case "search_products" -> executeSearchProducts(args);

            case "get_product_detail" -> executeGetProductDetail(args);

            case "get_my_orders" -> executeGetMyOrders();

            case "get_order_detail" -> executeGetOrderDetail(args);

            case "get_inventory" -> executeGetInventory(args);

            case "get_inventory_summary" -> executeGetInventorySummary();

            case "get_low_stock_products" -> executeGetLowStockProducts();

            case "list_public_vouchers" -> executeListPublicVouchers(args);

            case "validate_voucher" -> executeValidateVoucher(args);

            default -> Map.of(
                "error", "Unknown tool: " + functionName
            );
        };
    }

    private Map<String, Object> executeSearchProducts(
        Map<String, Object> args
    ) {

        String keyword = args.get("keyword") == null
            ? ""
            : args.get("keyword").toString().trim();
        java.math.BigDecimal minPrice = toBigDecimal(args.get("minPrice"));
        java.math.BigDecimal maxPrice = toBigDecimal(args.get("maxPrice"));

        if (!StringUtils.hasText(keyword) && minPrice == null && maxPrice == null) {
            return Map.of(
                "error",
                "Provide keyword and/or minPrice/maxPrice"
            );
        }

        log.info(
            "AI search_products called with keyword={} minPrice={} maxPrice={}",
            keyword,
            minPrice,
            maxPrice
        );

        try {

            var products =
                productAiTool.searchProducts(keyword, minPrice, maxPrice);

            log.info(
                "AI search_products returned {} products",
                products.size()
            );

            return Map.of(
                "success", true,
                "keyword", keyword,
                "minPrice", minPrice != null ? minPrice : "",
                "maxPrice", maxPrice != null ? maxPrice : "",
                "count", products.size(),
                "products", products
            );

        } catch (Exception e) {

            log.error(
                "AI search_products failed for keyword: {}",
                keyword,
                e
            );

            return Map.of(
                "success", false,
                "error",
                "Product search failed."
            );
        }
    }

    private static java.math.BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return java.math.BigDecimal.valueOf(number.doubleValue());
            }
            String text = value.toString().trim().replace(",", "");
            if (text.isEmpty()) {
                return null;
            }
            return new java.math.BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> executeGetProductDetail(
        Map<String, Object> args
    ) {

        Object productIdValue =
            args.get("productId");

        if (productIdValue == null) {
            return Map.of(
                "error",
                "productId is required"
            );
        }

        Long productId;

        try {

            productId =
                ((Number) productIdValue).longValue();

        } catch (Exception e) {

            return Map.of(
                "error",
                "productId must be a number"
            );
        }

        log.info(
            "AI get_product_detail called with productId: {}",
            productId
        );

        try {

            var product =
                productAiTool.getProductDetail(productId);

            return Map.of(
                "success", true,
                "product", product
            );

        } catch (Exception e) {

            log.error(
                "AI get_product_detail failed for productId: {}",
                productId,
                e
            );

            return Map.of(
                "success", false,
                "error",
                "Product not found."
            );
        }
    }

    private AiChatResponse buildResponse(
        String content
    ) {

        return AiChatResponse.builder()
            .content(
                content == null || content.isBlank()
                    ? "Xin lỗi, tôi không thể tạo câu trả lời lúc này."
                    : content
            )
            .provider("google-gemini")
            .model(model)
            .fallback(false)
            .build();
    }

    private Map<String, Object> executeGetMyOrders() {

        log.info("AI get_my_orders called");

        try {

            var orders = orderAiTool.getMyOrders();

            log.info(
                "AI get_my_orders returned {} orders",
                orders.getContent().size()
            );

            return Map.of(
                "success", true,
                "orders", orders
            );

        } catch (Exception e) {

            log.error(
                "AI get_my_orders failed",
                e
            );

            return Map.of(
                "success", false,
                "error", "Failed to retrieve orders."
            );
        }
    }

    private Map<String, Object> executeGetOrderDetail(
        Map<String, Object> args
    ) {

        Object orderIdValue = args.get("orderId");

        if (orderIdValue == null) {
            return Map.of(
                "error", "orderId is required"
            );
        }

        Long orderId;

        try {
            orderId = ((Number) orderIdValue).longValue();
        } catch (Exception e) {
            return Map.of(
                "error", "orderId must be a number"
            );
        }

        log.info(
            "AI get_order_detail called with orderId: {}",
            orderId
        );

        try {
            var order = orderAiTool.getOrderDetail(orderId);
            return Map.of(
                "success", true,
                "order", order
            );
        } catch (Exception e) {
            log.error(
                "AI get_order_detail failed for orderId: {}",
                orderId,
                e
            );
            return Map.of(
                "success", false,
                "error", "Order not found."
            );
        }
    }

    private Map<String, Object> executeGetInventory(
        Map<String, Object> args
    ) {

        Object productIdValue = args.get("productId");

        if (productIdValue == null) {
            return Map.of(
                "success", false,
                "error", "productId is required"
            );
        }

        Long productId;

        try {
            productId = ((Number) productIdValue).longValue();
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", "productId must be a number"
            );
        }

        log.info(
            "AI get_inventory called with productId: {}",
            productId
        );

        try {

            var inventory =
                inventoryAiTool.getInventory(productId);

            return Map.of(
                "success", true,
                "inventory", inventory
            );

        } catch (Exception e) {

            log.error(
                "AI get_inventory failed for productId: {}",
                productId,
                e
            );

            return Map.of(
                "success", false,
                "error", "Inventory not found."
            );
        }
    }

    private Map<String, Object> executeGetInventorySummary() {

        log.info("AI get_inventory_summary called");

        try {

            var summary =
                inventoryAiTool.getInventorySummary();

            return Map.of(
                "success", true,
                "summary", summary
            );

        } catch (Exception e) {

            log.error(
                "AI get_inventory_summary failed",
                e
            );

            return Map.of(
                "success", false,
                "error", "Failed to retrieve inventory summary."
            );
        }
    }

    private Map<String, Object> executeGetLowStockProducts() {

        log.info("AI get_low_stock_products called");

        try {

            var products =
                inventoryAiTool.getLowStockProducts();

            return Map.of(
                "success", true,
                "count", products.size(),
                "products", products
            );

        } catch (Exception e) {

            log.error(
                "AI get_low_stock_products failed",
                e
            );

            return Map.of(
                "success", false,
                "error", "Failed to retrieve low-stock products."
            );
        }
    }

    private Map<String, Object> executeListPublicVouchers(
        Map<String, Object> args
    ) {

        Long sellerId = null;

        Object sellerIdValue = args.get("sellerId");

        if (sellerIdValue != null) {
            try {
                sellerId = ((Number) sellerIdValue).longValue();
            } catch (Exception e) {
                return Map.of(
                    "success", false,
                    "error", "sellerId must be a number"
                );
            }
        }

        log.info(
            "AI list_public_vouchers called with sellerId: {}",
            sellerId
        );

        try {

            var vouchers =
                voucherAiTool.listPublicVouchers(sellerId);

            log.info(
                "AI list_public_vouchers returned {} vouchers",
                vouchers.size()
            );

            return Map.of(
                "success", true,
                "count", vouchers.size(),
                "vouchers", vouchers
            );

        } catch (Exception e) {

            log.error(
                "AI list_public_vouchers failed",
                e
            );

            return Map.of(
                "success", false,
                "error", "Failed to retrieve public vouchers."
            );
        }
    }

    private Map<String, Object> executeValidateVoucher(
        Map<String, Object> args
    ) {

        Object codeValue = args.get("code");

        if (codeValue == null) {
            return Map.of(
                "success", false,
                "error", "code is required"
            );
        }

        String code = codeValue.toString().trim();

        List<Long> productIds = List.of();

        Object productIdsValue = args.get("productIds");

        if (productIdsValue instanceof List<?> list) {

            try {

                productIds = list.stream()
                    .map(value -> ((Number) value).longValue())
                    .toList();

            } catch (Exception e) {

                return Map.of(
                    "success", false,
                    "error", "productIds must contain numbers"
                );
            }
        }

        log.info(
            "AI validate_voucher called with code: {} productIds: {}",
            code,
            productIds
        );

        try {

            var result =
                voucherAiTool.validateVoucher(
                    code,
                    productIds
                );

            return Map.of(
                "success", true,
                "validation", result
            );

        } catch (Exception e) {

            log.error(
                "AI validate_voucher failed for code: {}",
                code,
                e
            );

            return Map.of(
                "success", false,
                "error", "Voucher validation failed."
            );
        }
    }
}
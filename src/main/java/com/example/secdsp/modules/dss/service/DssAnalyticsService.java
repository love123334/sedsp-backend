package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.config.PowerBiProperties;
import com.example.secdsp.modules.ai.service.HuggingFaceChatService;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastProductView;
import com.example.secdsp.modules.dss.dto.DemandForecastResponse;
import com.example.secdsp.modules.dss.dto.DssInsightPlanResponse;
import com.example.secdsp.modules.dss.dto.InventoryRecommendationResponse;
import com.example.secdsp.modules.inventory.entity.Inventory;
import com.example.secdsp.modules.inventory.repository.InventoryRepository;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DssAnalyticsService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final HuggingFaceChatService huggingFaceChatService;
    private final PowerBiProperties powerBiProperties;
    private final DemandForecastEngine demandForecastEngine;

    private Long requireUserId() {
        Long id = SecurityUtils.getCurrentUserId();
        if (id == null) {
            throw new UnauthorizedException("Authentication required.");
        }
        return id;
    }

    private Product requireSellerProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (product.getSeller() == null || !product.getSeller().getId().equals(sellerId)) {
            if (!SecurityUtils.hasRole(UserRole.ADMIN) && !SecurityUtils.hasRole(UserRole.MANAGER)) {
                throw new UnauthorizedException("Product does not belong to current seller.");
            }
        }
        return product;
    }

    @Transactional(readOnly = true)
    public DemandForecastResponse forecastDemand(Long productId, int historyDays, int forecastDays) {
        Long sellerId = requireUserId();
        Product product = requireSellerProduct(productId, sellerId);
        DemandForecastProductView productView = new DemandForecastProductView(
            product.getId(),
            product.getSeller() != null ? product.getSeller().getId() : sellerId,
            product.getName(),
            product.getPrice()
        );
        var forecast = demandForecastEngine.forecast(
            productView,
            historyDays,
            forecastDays
        );

        return DemandForecastResponse.builder()
            .productId(forecast.productId())
            .productName(forecast.productName())
            .historicalDays(forecast.historicalDays())
            .forecastDays(forecast.forecastDays())
            .averageDailyDemand(forecast.averageDailyDemand())
            .predictedDemand(forecast.predictedDemand())
            .method(forecast.method())
            .insufficientData(forecast.insufficientData())
            .historicalSales(forecast.historicalSales())
            .forecastSales(forecast.forecastSales())
            .featureSnapshot(forecast.featureSnapshot())
            .generatedAt(forecast.generatedAt())
            .build();
    }

    @Transactional(readOnly = true)
    public InventoryRecommendationResponse recommendInventory(Long productId, int planningDays) {
        Long sellerId = requireUserId();
        int plan = clamp(planningDays, 7, 60);

        List<Product> products;
        if (productId != null) {
            products = List.of(requireSellerProduct(productId, sellerId));
        } else {
            products = productRepository.findBySeller_Id(sellerId, PageRequest.of(0, 20)).getContent();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        boolean needAny = false;

        for (Product p : products) {
            List<Object[]> daily = orderItemRepository.findDailySoldQuantity(sellerId, p.getId(), 30);
            double avgDaily = daily.isEmpty()
                ? 2.0
                : daily.stream().mapToLong(r -> ((Number) r[1]).longValue()).average().orElse(2.0);

            int stock = inventoryRepository.findByProduct_Id(p.getId())
                .map(Inventory::getAvailableQuantity)
                .orElse(0);
            int leadTime = 7;
            int safety = (int) Math.ceil(avgDaily * 3);
            int rop = (int) Math.ceil(avgDaily * leadTime) + safety;
            int recommendedOrder = Math.max(0, (int) Math.ceil(avgDaily * plan) + safety - stock);
            String status = stock < rop ? "need" : "sufficient";
            if ("need".equals(status)) {
                needAny = true;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", p.getId());
            row.put("productName", p.getName());
            row.put("currentStock", stock);
            row.put("averageDailyDemand", round1(avgDaily));
            row.put("leadTimeDays", leadTime);
            row.put("safetyStock", safety);
            row.put("reorderPoint", rop);
            row.put("recommendedOrder", recommendedOrder);
            row.put("status", status);
            row.put("statusLabel", "need".equals(status) ? "Can bo sung" : "Ton kho du");
            List<Map<String, Object>> historicalSales = new ArrayList<>();
            int dayIdx = 1;
            for (Object[] d : daily) {
                Map<String, Object> pt = new LinkedHashMap<>();
                pt.put("day", dayIdx++);
                pt.put("qty", ((Number) d[1]).longValue());
                historicalSales.add(pt);
            }
            row.put("historicalSales", historicalSales);
            rows.add(row);
        }

        return InventoryRecommendationResponse.builder()
            .planningDays(plan)
            .overallStatus(needAny ? "need" : "sufficient")
            .recommendationMessage(
                needAny
                    ? "Mot so SKU duoi diem dat hang lai (ROP). Nen nhap hang trong ky " + plan + " ngay."
                    : "Ton kho dang du so voi ROP trong ky " + plan + " ngay."
            )
            .rows(rows)
            .generatedAt(now())
            .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> powerBiSalesFeed(int limit) {
        Long userId = requireUserId();
        Long sellerFilter = SecurityUtils.hasRole(UserRole.MANAGER) || SecurityUtils.hasRole(UserRole.ADMIN)
            ? null
            : userId;

        int lim = clamp(limit, 50, 5000);
        List<Object[]> rows = sellerFilter == null
            ? orderItemRepository.findPowerBiSalesRowsAll(lim)
            : orderItemRepository.findPowerBiSalesRowsBySeller(sellerFilter, lim);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId", r[0]);
            m.put("createdAt", r[1] != null ? String.valueOf(r[1]) : null);
            m.put("status", r[2]);
            m.put("orderTotal", r[3]);
            m.put("productId", r[4]);
            m.put("productName", r[5]);
            m.put("quantity", r[6]);
            m.put("unitPrice", r[7]);
            m.put("subtotal", r[8]);
            m.put("sellerId", r[9]);
            out.add(m);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public DssInsightPlanResponse buildInsightPlan() {
        Long sellerId = requireUserId();
        InventoryRecommendationResponse inv = recommendInventory(null, 14);
        List<Object[]> top = orderItemRepository.findTopSellingProducts(sellerId);
        List<Map<String, Object>> topProducts = new ArrayList<>();
        for (Object[] row : top.stream().limit(5).toList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", row[0]);
            m.put("name", row[1]);
            m.put("qty", row[2]);
            m.put("revenue", row[3]);
            topProducts.add(m);
        }

        long lowStock = inv.getRows().stream().filter(r -> "need".equals(r.get("status"))).count();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("sellerId", sellerId);
        metrics.put("inventoryOverall", inv.getOverallStatus());
        metrics.put("inventoryMessage", inv.getRecommendationMessage());
        metrics.put("lowStockCount", lowStock);
        metrics.put("topProducts", topProducts);

        // Brief tiếng Việt cho AI — tránh field kỹ thuật / endpoint làm model "xì" ra UI
        String brief = buildSellerInsightBrief(inv.getOverallStatus(), inv.getRecommendationMessage(), lowStock, topProducts);
        String commentary = huggingFaceChatService.generateInsightPlan(brief);
        String source = huggingFaceChatService.isConfigured()
            ? "ai+sedsp-metrics"
            : "rule-based+sedsp-metrics";

        String embed = powerBiProperties.getEmbedUrl();
        if (embed == null) {
            embed = "";
        }

        return DssInsightPlanResponse.builder()
            .source(source)
            .commentary(commentary)
            .metrics(metrics)
            .powerBiEmbedUrl(embed)
            .powerBiReportTitle(powerBiProperties.getReportTitle())
            .powerBiFeedHint(
                "Power BI Desktop: Get Data → Web → "
                    + "GET {BACKEND}/api/v1/analytics/powerbi/sales (Bearer JWT). "
                    + "Publish report then set POWERBI_EMBED_URL."
            )
            .generatedAt(now())
            .build();
    }

    private static String buildSellerInsightBrief(
        String inventoryOverall,
        String inventoryMessage,
        long lowStockCount,
        List<Map<String, Object>> topProducts
    ) {
        String statusVi = switch (inventoryOverall == null ? "" : inventoryOverall) {
            case "need" -> "cần bổ sung hàng";
            case "ok", "sufficient" -> "ổn định";
            case "overstock" -> "tồn cao";
            default -> inventoryOverall == null || inventoryOverall.isBlank() ? "chưa rõ" : inventoryOverall;
        };

        StringBuilder sb = new StringBuilder();
        sb.append("Tóm tắt kinh doanh (dùng để viết nhận xét cho người bán):\n");
        sb.append("- Tình trạng tồn kho tổng thể: ").append(statusVi).append('\n');
        sb.append("- Số mặt hàng cần nhập thêm: ").append(lowStockCount).append('\n');
        if (inventoryMessage != null && !inventoryMessage.isBlank()) {
            sb.append("- Gợi ý tồn kho: ").append(inventoryMessage).append('\n');
        }
        sb.append("- Sản phẩm bán chạy:\n");
        if (topProducts == null || topProducts.isEmpty()) {
            sb.append("  (chưa có đơn giao thành công gần đây)\n");
        } else {
            int i = 1;
            for (Map<String, Object> p : topProducts) {
                sb.append("  ").append(i++).append(". ")
                    .append(String.valueOf(p.get("name")))
                    .append(" — đã bán ").append(String.valueOf(p.get("qty")))
                    .append(" sp, doanh thu ").append(String.valueOf(p.get("revenue")))
                    .append("đ\n");
            }
        }
        sb.append("- Gợi ý module DSS nên nhắc: dự báo LightGBM, gợi ý giá nâng cao, hiệu quả đơn hàng, khuyến nghị tồn kho.\n");
        return sb.toString();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

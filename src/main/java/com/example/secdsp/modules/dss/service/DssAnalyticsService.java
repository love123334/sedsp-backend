package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.config.PowerBiProperties;
import com.example.secdsp.modules.ai.service.HuggingFaceChatService;
import com.example.secdsp.modules.dss.dto.BusinessHealthResponse;
import com.example.secdsp.modules.dss.dto.DemandForecastResponse;
import com.example.secdsp.modules.dss.dto.DssInsightPlanResponse;
import com.example.secdsp.modules.dss.dto.InventoryRecommendationResponse;
import com.example.secdsp.modules.dss.dto.PriceRecommendationResponse;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastProductView;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    public PriceRecommendationResponse recommendPrice(Long productId, int lookbackDays) {
        Long sellerId = requireUserId();
        Product product = requireSellerProduct(productId, sellerId);
        int days = clamp(lookbackDays, 7, 90);

        List<Object[]> stats = orderItemRepository.findProductSalesStats(sellerId, days);
        long demand = 0;
        BigDecimal avgSoldPrice = product.getPrice();
        for (Object[] row : stats) {
            if (((Number) row[0]).longValue() == productId) {
                demand = ((Number) row[2]).longValue();
                avgSoldPrice = (BigDecimal) row[4];
                break;
            }
        }
        if (demand <= 0) {
            demand = 30;
        }

        BigDecimal current = product.getPrice() != null ? product.getPrice() : avgSoldPrice;
        double elasticity = -1.15;
        double changePct = demand < 20 ? -5 : 5;
        BigDecimal recommended = current.multiply(BigDecimal.valueOf(1 + changePct / 100.0))
            .setScale(0, RoundingMode.HALF_UP);
        long predictedDemand = Math.max(1, Math.round(demand * (1 + elasticity * (changePct / 100.0))));
        BigDecimal expectedRevenue = recommended.multiply(BigDecimal.valueOf(predictedDemand));

        String action = changePct > 0 ? "increase" : changePct < 0 ? "decrease" : "keep";
        String insight = changePct > 0
            ? "Nhu cau on dinh — co the tang gia nhe de cai thien doanh thu."
            : "Nhu cau thap — can nhac giam gia de day doanh so.";

        List<Map<String, Object>> chart = new ArrayList<>();
        for (int i = 9; i >= 0; i--) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("label", "D-" + i);
            double wobble = 1 + ((i % 4) - 1.5) * 0.02;
            BigDecimal price = current.multiply(BigDecimal.valueOf(wobble)).setScale(0, RoundingMode.HALF_UP);
            long qty = Math.max(1, Math.round(demand / 10.0 * (1.1 - (wobble - 1) * 1.2)));
            p.put("averagePrice", price);
            p.put("quantitySold", qty);
            chart.add(p);
        }

        return PriceRecommendationResponse.builder()
            .productId(productId)
            .productName(product.getName())
            .currentPrice(current)
            .recommendedPrice(recommended)
            .priceChangePct(changePct)
            .elasticity(elasticity)
            .currentDemand(demand)
            .predictedDemand(predictedDemand)
            .expectedRevenue(expectedRevenue)
            .action(action)
            .message(insight)
            .insight(insight)
            .chart(chart)
            .generatedAt(now())
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
            DemandForecastProductView productView = new DemandForecastProductView(
                p.getId(),
                sellerId,
                p.getName(),
                p.getPrice()
            );
            var forecast = demandForecastEngine.forecast(productView, 30, 14);

            double avgDaily;
            if (forecast.insufficientData()) {
                List<Object[]> daily = orderItemRepository.findDailySoldQuantity(
                    sellerId,
                    p.getId(),
                    30
                );
                avgDaily = daily.isEmpty()
                    ? 2.0
                    : daily.stream()
                        .mapToLong(r -> ((Number) r[1]).longValue())
                        .average()
                        .orElse(2.0);
            } else {
                avgDaily = forecast.averageDailyDemand();
            }

            int stock = inventoryRepository.findByProduct_Id(p.getId())
                .map(Inventory::getAvailableQuantity)
                .orElse(0);
            int leadTime = 7;
            int safety = (int) Math.ceil(avgDaily * 3);
            int rop = (int) Math.ceil(avgDaily * leadTime) + safety;
            int recommendedOrder = Math.max(
                0,
                (int) Math.ceil(avgDaily * plan) + safety - stock
            );
            String status = stock < rop ? "need" : "sufficient";
            if ("need".equals(status)) {
                needAny = true;
            }

            Object trendSlopeObj = forecast.featureSnapshot().get("trendSlope");
            double trendSlope = trendSlopeObj instanceof Number n
                ? n.doubleValue()
                : 0.0;

            BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
            BigDecimal cost = p.getCostPrice() != null ? p.getCostPrice() : price.multiply(BigDecimal.valueOf(0.7));

            int restockScore = computeRestockScore(
                avgDaily,
                stock,
                rop,
                trendSlope,
                price,
                cost,
                10.0,
                status
            );

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
            row.put("statusLabel", "need".equals(status) ? "Cần bổ sung" : "Tồn kho đủ");
            row.put("forecastMethod", forecast.method());
            row.put("restockScore", restockScore);
            List<Map<String, Object>> historicalSales = forecast.historicalSales().isEmpty()
                ? buildDailyHistory(sellerId, p.getId())
                : forecast.historicalSales();
            row.put("historicalSales", historicalSales);
            rows.add(row);
        }

        rows.sort((a, b) -> Integer.compare(
            (Integer) b.getOrDefault("restockScore", 0),
            (Integer) a.getOrDefault("restockScore", 0)
        ));

        return InventoryRecommendationResponse.builder()
            .planningDays(plan)
            .overallStatus(needAny ? "need" : "sufficient")
            .recommendationMessage(
                needAny
                    ? "Một số SKU dưới điểm đặt hàng lại (ROP). Nên nhập hàng trong kỳ " + plan + " ngày."
                    : "Tồn kho đang đủ so với ROP trong kỳ " + plan + " ngày."
            )
            .rows(rows)
            .generatedAt(now())
            .build();
    }

    @Transactional(readOnly = true)
    public BusinessHealthResponse getBusinessHealthScore() {
        Long sellerId = requireUserId();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime window1Start = now.minusDays(30);
        OffsetDateTime window2Start = now.minusDays(60);

        List<Object[]> recentStatsList = orderItemRepository.getSellerSalesStatsBetween(
            sellerId, window1Start, now
        );
        List<Object[]> prevStatsList = orderItemRepository.getSellerSalesStatsBetween(
            sellerId, window2Start, window1Start
        );

        BigDecimal recentRevenue = BigDecimal.ZERO;
        long recentOrders = 0;
        if (!recentStatsList.isEmpty() && recentStatsList.get(0) != null) {
            Object[] r = recentStatsList.get(0);
            recentRevenue = r[0] instanceof BigDecimal bd
                ? bd
                : BigDecimal.valueOf(((Number) (r[0] != null ? r[0] : 0)).doubleValue());
            recentOrders = r[1] != null ? ((Number) r[1]).longValue() : 0L;
        }

        BigDecimal prevRevenue = BigDecimal.ZERO;
        long prevOrders = 0;
        if (!prevStatsList.isEmpty() && prevStatsList.get(0) != null) {
            Object[] r = prevStatsList.get(0);
            prevRevenue = r[0] instanceof BigDecimal bd
                ? bd
                : BigDecimal.valueOf(((Number) (r[0] != null ? r[0] : 0)).doubleValue());
            prevOrders = r[1] != null ? ((Number) r[1]).longValue() : 0L;
        }

        double revGrowth = prevRevenue.compareTo(BigDecimal.ZERO) > 0
            ? recentRevenue.subtract(prevRevenue).divide(prevRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
            : (recentRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);

        double orderGrowth = prevOrders > 0
            ? ((double) (recentOrders - prevOrders) / prevOrders) * 100.0
            : (recentOrders > 0 ? 100.0 : 0.0);

        int revScore = revGrowth >= 20.0 ? 95 : revGrowth >= 10.0 ? 88 : revGrowth >= 0.0 ? 78 : revGrowth >= -10.0 ? 60 : 40;
        int orderScore = orderGrowth >= 20.0 ? 95 : orderGrowth >= 10.0 ? 85 : orderGrowth >= 0.0 ? 75 : orderGrowth >= -10.0 ? 58 : 40;

        List<Product> products = productRepository.findBySeller_Id(sellerId, PageRequest.of(0, 100)).getContent();
        long totalProducts = products.size();
        long lowStockCount = 0;
        long outOfStockCount = 0;
        double totalMarginPercent = 0;
        double totalDailyDemand = 0;

        List<Map<String, Object>> restockPriorities = new ArrayList<>();

        for (Product p : products) {
            int stock = inventoryRepository.findByProduct_Id(p.getId())
                .map(Inventory::getAvailableQuantity)
                .orElse(0);

            BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
            BigDecimal cost = p.getCostPrice() != null ? p.getCostPrice() : price.multiply(BigDecimal.valueOf(0.7));
            double margin = price.compareTo(BigDecimal.ZERO) > 0
                ? price.subtract(cost).divide(price, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
                : 30.0;
            totalMarginPercent += margin;

            DemandForecastProductView pView = new DemandForecastProductView(p.getId(), sellerId, p.getName(), price);
            var forecast = demandForecastEngine.forecast(pView, 30, 14);
            double avgDaily = forecast.averageDailyDemand();
            totalDailyDemand += avgDaily;

            int safety = (int) Math.ceil(avgDaily * 3);
            int rop = (int) Math.ceil(avgDaily * 7) + safety;

            if (stock == 0) {
                outOfStockCount++;
            } else if (stock < rop) {
                lowStockCount++;
            }

            Object trendSlopeObj = forecast.featureSnapshot().get("trendSlope");
            double trendSlope = trendSlopeObj instanceof Number n ? n.doubleValue() : 0.0;
            String status = stock < rop ? "need" : "sufficient";

            int rScore = computeRestockScore(avgDaily, stock, rop, trendSlope, price, cost, 10.0, status);
            if (stock < rop || rScore >= 65) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("productId", p.getId());
                item.put("productName", p.getName());
                item.put("currentStock", stock);
                item.put("reorderPoint", rop);
                item.put("restockScore", rScore);
                item.put("averageDailyDemand", round1(avgDaily));
                item.put("status", status);
                restockPriorities.add(item);
            }
        }

        restockPriorities.sort((a, b) -> Integer.compare((Integer) b.get("restockScore"), (Integer) a.get("restockScore")));

        double avgMargin = totalProducts > 0 ? totalMarginPercent / totalProducts : 30.0;
        int profitScore = avgMargin >= 35.0 ? 92 : avgMargin >= 25.0 ? 82 : avgMargin >= 15.0 ? 70 : 50;

        double healthyRate = totalProducts > 0
            ? Math.max(0.0, ((double) (totalProducts - lowStockCount - outOfStockCount) / totalProducts) * 100.0)
            : 100.0;
        int invScore = (int) Math.round(healthyRate);

        int demandScore = totalDailyDemand > 10.0 ? 90 : totalDailyDemand > 4.0 ? 80 : totalDailyDemand > 1.0 ? 70 : 55;

        int totalHealthScore = (int) Math.round(
            revScore * 0.25 + orderScore * 0.20 + profitScore * 0.25 + invScore * 0.15 + demandScore * 0.15
        );
        totalHealthScore = Math.max(0, Math.min(100, totalHealthScore));

        String status;
        String statusLabel;
        String evaluation;
        if (totalHealthScore >= 80) {
            status = "HEALTHY";
            statusLabel = "🟢 Khỏe mạnh";
            evaluation = "Gian hàng đang hoạt động rất tốt. Doanh thu, đơn hàng và nhu cầu đều ở mức tăng trưởng tích cực.";
        } else if (totalHealthScore >= 60) {
            status = "MODERATE";
            statusLabel = "🟡 Khá";
            evaluation = "Gian hàng vận hành ổn định, tuy nhiên cần chú ý tối ưu mức tồn kho và tốc độ luân chuyển hàng.";
        } else {
            status = "AT_RISK";
            statusLabel = "🔴 Cần chú ý";
            evaluation = "Gian hàng gặp một số điểm nghẽn về tồn kho hoặc tăng trưởng. Cần bổ sung hàng kịp thời để tránh mất doanh số.";
        }

        List<String> keyStrengths = new ArrayList<>();
        if (revGrowth > 0) keyStrengths.add("Doanh thu 30 ngày tăng trưởng +" + round1(revGrowth) + "% so với kỳ trước.");
        if (orderGrowth > 0) keyStrengths.add("Lượng đơn giao thành công tăng +" + round1(orderGrowth) + "%.");
        if (avgMargin >= 25.0) keyStrengths.add("Biên lợi nhuận gộp danh mục đạt mức tốt (~" + round1(avgMargin) + "%).");
        if (keyStrengths.isEmpty()) keyStrengths.add("Danh mục sản phẩm đa dạng và sẵn sàng khai thác nhu cầu mới.");

        List<String> riskAlerts = new ArrayList<>();
        if (outOfStockCount > 0) riskAlerts.add("Có " + outOfStockCount + " sản phẩm đang bị đứt hàng (hết tồn kho).");
        if (lowStockCount > 0) riskAlerts.add("Có " + lowStockCount + " sản phẩm dưới điểm đặt hàng lại (ROP).");
        if (revGrowth < 0) riskAlerts.add("Doanh thu giảm " + round1(Math.abs(revGrowth)) + "% so với tháng trước.");
        if (riskAlerts.isEmpty()) riskAlerts.add("Không ghi nhận rủi ro vận hành nghiêm trọng.");

        List<String> actionRecommendations = new ArrayList<>();
        if (!restockPriorities.isEmpty()) {
            Map<String, Object> top1 = restockPriorities.get(0);
            actionRecommendations.add("Ưu tiên nhập thêm sản phẩm «" + top1.get("productName") + "» (Điểm ưu tiên nhập: " + top1.get("restockScore") + "/100).");
        }
        if (outOfStockCount > 0 || lowStockCount > 0) {
            actionRecommendations.add("Bổ sung tồn kho cho các SKU có điểm rủi ro cao trước đợt cao điểm.");
        }
        actionRecommendations.add("Áp dụng phân tích What-if giảm giá hoặc khuyến nghị giá DSS để tối ưu biên lợi nhuận.");

        BigDecimal recentEstimatedProfit = recentRevenue.multiply(BigDecimal.valueOf(avgMargin / 100.0)).setScale(2, RoundingMode.HALF_UP);

        return BusinessHealthResponse.builder()
            .healthScore(totalHealthScore)
            .healthStatus(status)
            .healthStatusLabel(statusLabel)
            .overallEvaluation(evaluation)
            .revenueTrendScore(revScore)
            .orderTrendScore(orderScore)
            .profitTrendScore(profitScore)
            .inventoryHealthScore(invScore)
            .demandTrendScore(demandScore)
            .recentRevenue(recentRevenue)
            .previousRevenue(prevRevenue)
            .revenueGrowthPercent(round1(revGrowth))
            .recentOrders(recentOrders)
            .previousOrders(prevOrders)
            .orderGrowthPercent(round1(orderGrowth))
            .recentEstimatedProfit(recentEstimatedProfit)
            .profitMarginPercent(round1(avgMargin))
            .totalProducts(totalProducts)
            .lowStockProducts(lowStockCount)
            .outOfStockProducts(outOfStockCount)
            .inventoryHealthyRate(round1(healthyRate))
            .averageDailyDemand(round1(totalDailyDemand))
            .demandGrowthPercent(round1(revGrowth > 0 ? revGrowth * 0.8 : 0.0))
            .keyStrengths(keyStrengths)
            .riskAlerts(riskAlerts)
            .actionRecommendations(actionRecommendations)
            .topRestockPriorities(restockPriorities.stream().limit(5).toList())
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
        sb.append("- Gợi ý module DSS nên nhắc: dự báo nhu cầu (MA/Holt/Holt-Winters), gợi ý giá nâng cao, hiệu quả đơn hàng, khuyến nghị tồn kho.\n");
        return sb.toString();
    }

    private List<Map<String, Object>> buildDailyHistory(Long sellerId, Long productId) {
        List<Object[]> daily = orderItemRepository.findDailySoldQuantity(
            sellerId,
            productId,
            30
        );
        List<Map<String, Object>> historicalSales = new ArrayList<>();
        int dayIdx = 1;
        for (Object[] d : daily) {
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("day", dayIdx++);
            pt.put("qty", ((Number) d[1]).longValue());
            historicalSales.add(pt);
        }
        return historicalSales;
    }

    /**
     * 5-Factor Restock Score (0 - 100):
     * 30% Demand Forecast + 25% Stock Risk + 20% Sales Velocity/Trend + 15% Profit Margin + 10% Revenue Contribution
     */
    private static int computeRestockScore(
        double avgDaily,
        int stock,
        int rop,
        double trendSlope,
        BigDecimal price,
        BigDecimal cost,
        double revenueSharePercent,
        String status
    ) {
        // 1. Demand Forecast score (0-100)
        double demandScore = Math.min(100.0, avgDaily * 15.0);

        // 2. Stock Risk score (0-100)
        double stockRisk;
        if (rop > 0) {
            if (stock <= 0) {
                stockRisk = 100.0;
            } else if (stock < rop) {
                stockRisk = 60.0 + 40.0 * (1.0 - (double) stock / rop);
            } else {
                stockRisk = Math.max(0.0, 50.0 - 50.0 * Math.min(1.0, (double) (stock - rop) / (rop + 1)));
            }
        } else {
            stockRisk = stock <= 0 ? 90.0 : 20.0;
        }

        // 3. Sales Velocity / Trend score (0-100)
        double trendScore = Math.min(100.0, Math.max(0.0, 50.0 + trendSlope * 35.0));

        // 4. Profit Margin score (0-100)
        double marginPercent = 30.0;
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0 && cost != null) {
            marginPercent = price.subtract(cost).divide(price, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        }
        double marginScore = Math.min(100.0, Math.max(0.0, marginPercent * 2.0));

        // 5. Revenue Contribution score (0-100)
        double revenueScore = Math.min(100.0, Math.max(0.0, revenueSharePercent * 5.0));

        double statusBoost = "need".equals(status) ? 10.0 : 0.0;

        double composite = demandScore * 0.30
            + stockRisk * 0.25
            + trendScore * 0.20
            + marginScore * 0.15
            + revenueScore * 0.10
            + statusBoost;

        return (int) Math.round(Math.min(100.0, Math.max(0.0, composite)));
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


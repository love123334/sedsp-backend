package com.example.secdsp.modules.ai.tool;

import com.example.secdsp.modules.dss.dto.BusinessHealthResponse;
import com.example.secdsp.modules.dss.dto.DemandForecastResponse;
import com.example.secdsp.modules.dss.dto.InventoryRecommendationResponse;
import com.example.secdsp.modules.dss.dto.PriceRecommendationResponse;
import com.example.secdsp.modules.dss.dto.request.SellerDiscountAnalysisRequest;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import com.example.secdsp.modules.dss.service.DssAnalyticsService;
import com.example.secdsp.modules.dss.service.SellerWhatIfAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DssAiTool {

    private final DssAnalyticsService dssAnalyticsService;
    private final SellerWhatIfAnalysisService sellerWhatIfAnalysisService;

    public Map<String, Object> getDemandForecast(Long productId, Integer forecastDays) {
        int horizon = (forecastDays != null && forecastDays > 0) ? forecastDays : 30;
        DemandForecastResponse forecast = dssAnalyticsService.forecastDemand(productId, 180, horizon);

        Map<String, Object> snapshot = forecast.getFeatureSnapshot() != null
            ? forecast.getFeatureSnapshot()
            : Map.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", forecast.getProductId());
        result.put("productName", forecast.getProductName());
        result.put("forecastDays", forecast.getForecastDays());
        result.put("historicalDays", forecast.getHistoricalDays());
        result.put("averageDailyDemand", forecast.getAverageDailyDemand());
        result.put("predictedTotalDemand", forecast.getPredictedDemand());
        result.put("forecastingMethod", forecast.getMethod());
        result.put("historyTrendLabel", snapshot.get("historyTrendLabel"));
        result.put("forecastTrendLabel", snapshot.get("forecastTrendLabel"));
        result.put("trendInsightLabel", snapshot.get("trendInsightLabel"));
        result.put("trendCombined", snapshot.get("trendCombined"));
        result.put("trendInsightDetail", snapshot.get("trendInsightDetail"));
        result.put("trendRecommendation", snapshot.get("trendRecommendation"));
        result.put("insufficientData", forecast.isInsufficientData());
        result.put("featureSnapshot", snapshot);
        return result;
    }

    public Map<String, Object> getRestockRecommendations(Integer planningDays, Long productId) {
        int horizon = (planningDays != null && planningDays > 0) ? planningDays : 14;
        InventoryRecommendationResponse inv = dssAnalyticsService.recommendInventory(productId, horizon);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planningDays", inv.getPlanningDays());
        result.put("overallStatus", inv.getOverallStatus());
        result.put("recommendationMessage", inv.getRecommendationMessage());
        result.put("rankedProducts", inv.getRows());
        return result;
    }

    public Map<String, Object> getWhatIfDiscountAnalysis(Long productId, Double priceChangePercent, Integer simulationPeriod) {
        int period = (simulationPeriod != null && simulationPeriod > 0) ? simulationPeriod : 30;
        BigDecimal pct = BigDecimal.valueOf(priceChangePercent != null ? priceChangePercent : 0.0);

        SellerDiscountAnalysisRequest request = new SellerDiscountAnalysisRequest();
        request.setProductId(productId);
        request.setPriceChangePercent(pct);
        request.setSimulationPeriod(period);

        SellerDiscountAnalysisResponse analysis = sellerWhatIfAnalysisService.analyzeDiscount(request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", productId);
        result.put("priceChangePercent", analysis.getPriceChangePercent());
        result.put("currentPrice", analysis.getCurrentPrice());
        result.put("costPrice", analysis.getCostPrice());
        result.put("newPrice", analysis.getNewPrice());
        result.put("baseDemand", analysis.getForecastDemand());
        result.put("expectedQuantity", analysis.getPredictedDemand());
        result.put("currentRevenue", analysis.getCurrentRevenue());
        result.put("expectedRevenue", analysis.getExpectedRevenue());
        BigDecimal cost = analysis.getCostPrice() != null ? analysis.getCostPrice() : BigDecimal.ZERO;
        long predQty = analysis.getPredictedDemand() != null ? analysis.getPredictedDemand() : 0L;
        result.put("expectedCOGS", cost.multiply(BigDecimal.valueOf(predQty)));
        result.put("currentProfit", analysis.getCurrentProfit());
        result.put("expectedProfit", analysis.getExpectedProfit());
        result.put("profitChangePercent", analysis.getProfitChangePercent());
        result.put("breakEvenQuantity", analysis.getBreakEvenQuantity());
        result.put("additionalUnitsRequired", analysis.getAdditionalUnitsRequired());
        result.put("businessInsight", analysis.getBusinessInsight());
        result.put("recommendation", analysis.getRecommendation());
        result.put("recommendationReason", analysis.getRecommendationReason());
        return result;
    }


    public Map<String, Object> getPriceRecommendation(Long productId, Integer lookbackDays) {
        int days = (lookbackDays != null && lookbackDays > 0) ? lookbackDays : 30;
        PriceRecommendationResponse resp = dssAnalyticsService.recommendPrice(productId, days);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", resp.getProductId());
        result.put("productName", resp.getProductName());
        result.put("currentPrice", resp.getCurrentPrice());
        result.put("recommendedPrice", resp.getRecommendedPrice());
        result.put("priceChangePct", resp.getPriceChangePct());
        result.put("elasticity", resp.getElasticity());
        result.put("currentDemand", resp.getCurrentDemand());
        result.put("predictedDemand", resp.getPredictedDemand());
        result.put("expectedRevenue", resp.getExpectedRevenue());
        result.put("action", resp.getAction());
        result.put("insight", resp.getInsight());
        return result;
    }

    public Map<String, Object> getBusinessHealth() {
        BusinessHealthResponse health = dssAnalyticsService.getBusinessHealthScore();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("healthScore", health.getHealthScore());
        result.put("healthStatus", health.getHealthStatus());
        result.put("healthStatusLabel", health.getHealthStatusLabel());
        result.put("overallEvaluation", health.getOverallEvaluation());
        result.put("scores", Map.of(
            "revenueTrendScore", health.getRevenueTrendScore(),
            "orderTrendScore", health.getOrderTrendScore(),
            "profitTrendScore", health.getProfitTrendScore(),
            "inventoryHealthScore", health.getInventoryHealthScore(),
            "demandTrendScore", health.getDemandTrendScore()
        ));
        result.put("metrics", Map.of(
            "recentRevenue", health.getRecentRevenue(),
            "revenueGrowthPercent", health.getRevenueGrowthPercent(),
            "recentOrders", health.getRecentOrders(),
            "orderGrowthPercent", health.getOrderGrowthPercent(),
            "profitMarginPercent", health.getProfitMarginPercent(),
            "lowStockProducts", health.getLowStockProducts(),
            "outOfStockProducts", health.getOutOfStockProducts()
        ));
        result.put("keyStrengths", health.getKeyStrengths());
        result.put("riskAlerts", health.getRiskAlerts());
        result.put("actionRecommendations", health.getActionRecommendations());
        result.put("topRestockPriorities", health.getTopRestockPriorities());
        return result;
    }
}

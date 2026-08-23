package com.example.secdsp.modules.dss.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BusinessHealthResponse {

    int healthScore;
    String healthStatus;
    String healthStatusLabel;
    String overallEvaluation;

    // 5 Pillars breakdown (0 - 100)
    int revenueTrendScore;
    int orderTrendScore;
    int profitTrendScore;
    int inventoryHealthScore;
    int demandTrendScore;

    // Key metrics for explainability
    BigDecimal recentRevenue;
    BigDecimal previousRevenue;
    double revenueGrowthPercent;

    long recentOrders;
    long previousOrders;
    double orderGrowthPercent;

    BigDecimal recentEstimatedProfit;
    double profitMarginPercent;

    long totalProducts;
    long lowStockProducts;
    long outOfStockProducts;
    double inventoryHealthyRate;

    double averageDailyDemand;
    double demandGrowthPercent;

    List<String> keyStrengths;
    List<String> riskAlerts;
    List<String> actionRecommendations;
    List<Map<String, Object>> topRestockPriorities;
    String generatedAt;
}

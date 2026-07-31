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
public class PriceRecommendationResponse {

    Long productId;
    String productName;
    BigDecimal currentPrice;
    BigDecimal recommendedPrice;
    double priceChangePct;
    double elasticity;
    long currentDemand;
    long predictedDemand;
    BigDecimal expectedRevenue;
    String action;
    String message;
    String insight;
    List<Map<String, Object>> chart;
    String generatedAt;
}

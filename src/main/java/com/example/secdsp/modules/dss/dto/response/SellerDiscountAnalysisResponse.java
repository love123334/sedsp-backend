package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerDiscountAnalysisResponse {

    BigDecimal currentPrice;

    BigDecimal costPrice;

    /** % thay đổi giá: âm giảm, dương tăng. */
    BigDecimal priceChangePercent;

    BigDecimal discountPercentage;

    BigDecimal newPrice;

    Long forecastDemand;

    Long predictedDemand;

    BigDecimal currentProfit;

    BigDecimal expectedProfit;

    Long breakEvenQuantity;

    Long additionalUnitsRequired;

    String businessInsight;

    Integer simulationPeriod;

    String historicalPeriodLabel;

    String forecastPeriodLabel;

    String methodology;

    BigDecimal currentRevenue;

    BigDecimal expectedRevenue;

    BigDecimal profitChangePercent;

    DssProfitBreakdownResponse currentProfitBreakdown;

    DssProfitBreakdownResponse expectedProfitBreakdown;

    String recommendation;

    String recommendationReason;
}

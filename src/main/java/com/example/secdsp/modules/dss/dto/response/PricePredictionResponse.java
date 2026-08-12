package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricePredictionResponse {

    Long productId;

    String productName;

    LocalDate fromDate;

    LocalDate toDate;

    BigDecimal currentPrice;

    BigDecimal cost;

    BigDecimal averageElasticity;

    Long totalQuantitySold;

    PriceScenarioResponse bestScenario;

    List<PriceScenarioResponse> scenarios;

    /** Kỳ dự báo áp dụng cho số lượng kịch bản (ngày). */
    Integer forecastPeriodDays;

    String historicalPeriodLabel;

    String forecastPeriodLabel;

    LocalDate forecastFrom;

    LocalDate forecastTo;

    String scenarioAssumptionNote;

    String recommendation;

    String recommendationReason;

    DssProfitBreakdownResponse currentSituationBreakdown;

    DssProductContextResponse productContext;

    List<DssPriceChangeImpactResponse> priceChangeImpacts;

    List<DssHolidayImpactResponse> upcomingHolidays;

    DssAiInsightResponse aiInsight;
}

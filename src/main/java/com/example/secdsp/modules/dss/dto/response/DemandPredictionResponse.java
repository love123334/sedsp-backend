package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DemandPredictionResponse {

    String productName;

    Integer historicalDays;

    Integer forecastPeriod;

    BigDecimal averageDailyDemand;

    BigDecimal predictedDemand;

    /** Dự báo có điều chỉnh thứ + ngày lễ */
    BigDecimal seasonalityAdjustedDemand;

    BigDecimal holidayAdjustmentFactor;

    LocalDateTime generatedAt;

    LocalDate historicalFrom;

    LocalDate historicalTo;

    String historicalPeriodLabel;

    String forecastPeriodLabel;

    String methodology;

    BigDecimal trendFactor;

    List<DssForecastDayResponse> forecastSeries;

    List<DssHolidayImpactResponse> upcomingHolidays;

    DssProductContextResponse productContext;

    List<DssPriceChangeImpactResponse> priceChangeImpacts;

    DssAiInsightResponse aiInsight;
}

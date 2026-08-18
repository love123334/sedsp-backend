package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdvancedPriceScenarioResponse {

    Long scenarioId;
    BigDecimal priceChangePercent;
    BigDecimal costPrice;
    BigDecimal newPrice;
    BigDecimal profitPerProduct;
    Long baselineForecastDemand;
    BigDecimal demandMultiplier;
    Long forecastDemand;
    BigDecimal expectedProfit;
    OffsetDateTime createdAt;
    OffsetDateTime appliedAt;
    boolean applied;
}


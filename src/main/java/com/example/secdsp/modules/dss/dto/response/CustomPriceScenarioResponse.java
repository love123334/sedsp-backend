package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomPriceScenarioResponse {

    Long productId;

    String productName;

    BigDecimal currentPrice;

    BigDecimal customPrice;

    /** % thay đổi suy ra từ giá tùy chỉnh. */
    BigDecimal derivedPriceChangePercent;

    Integer forecastPeriodDays;

    String forecastPeriodLabel;

    PriceScenarioResponse scenario;

    String recommendation;

    String recommendationReason;
}

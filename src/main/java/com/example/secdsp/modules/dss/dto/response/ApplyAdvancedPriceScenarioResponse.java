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
public class ApplyAdvancedPriceScenarioResponse {

    Long sessionId;
    Long scenarioId;
    Long productId;
    BigDecimal oldPrice;
    BigDecimal newPrice;
    BigDecimal priceChangePercent;
    OffsetDateTime appliedAt;
}


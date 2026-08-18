package com.example.secdsp.modules.dss.dto.response;

import com.example.secdsp.modules.dss.entity.AdvancedPriceSessionStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdvancedPriceSessionResponse {

    Long sessionId;
    AdvancedPriceSessionStatus status;
    AdvancedPriceProductSummaryResponse productSummary;
    BigDecimal averageElasticity;
    String elasticitySource;
    Long baselineForecastDemand;
    String forecastMethod;
    AdvancedPriceScenarioResponse latestScenario;
    List<AdvancedPriceScenarioResponse> scenarios;
    int scenarioCount;
    int maxScenarios;
    OffsetDateTime appliedAt;
    OffsetDateTime createdAt;
}

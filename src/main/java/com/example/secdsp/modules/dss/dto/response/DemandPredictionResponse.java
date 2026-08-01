package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    LocalDateTime generatedAt;
}

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
}

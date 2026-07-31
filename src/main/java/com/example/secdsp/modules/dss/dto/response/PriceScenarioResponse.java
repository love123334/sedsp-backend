package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceScenarioResponse {

    Integer priceChangePercent;

    BigDecimal cost;

    BigDecimal newPrice;

    BigDecimal profitPerProduct;

    Long predictedDemand;

    BigDecimal expectedProfit;
}

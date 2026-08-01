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

    BigDecimal discountPercentage;

    BigDecimal newPrice;

    Long forecastDemand;

    Long predictedDemand;

    BigDecimal currentProfit;

    BigDecimal expectedProfit;

    Long breakEvenQuantity;

    Long additionalUnitsRequired;

    String businessInsight;
}

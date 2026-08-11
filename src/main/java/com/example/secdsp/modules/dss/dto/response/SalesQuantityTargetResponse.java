package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesQuantityTargetResponse {

    Long productId;

    String productName;

    Integer simulationPeriod;

    String forecastPeriodLabel;

    Double increasePercent;

    /** Số lượng dự báo hiện tại trong kỳ mô phỏng. */
    Long currentForecastQuantity;

    /** Số lượng mục tiêu = current × (1 + increase%). */
    Long targetQuantity;

    BigDecimal currentPrice;

    /** Giá / khuyến mãi ước tính cần để đạt mục tiêu (theo co giãn cầu). */
    BigDecimal suggestedPrice;

    BigDecimal suggestedPriceChangePercent;

    DssProfitBreakdownResponse currentSituation;

    DssProfitBreakdownResponse targetSituation;

    BigDecimal profitChangePercent;

    String recommendation;

    String recommendationReason;

    String methodology;
}

package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetProfitAnalysisResponse {

    Long productId;

    String productName;

    Integer simulationPeriod;

    String forecastPeriodLabel;

    String historicalPeriodLabel;

    BigDecimal targetProfitVnd;

    /** Tình hình hiện tại (giá & dự báo nhu cầu). */
    BigDecimal currentPrice;

    Long forecastDemand;

    DssProfitBreakdownResponse currentSituation;

    /** Kịch bản gần đạt mục tiêu nhất trong dải % thay đổi giá cấu hình. */
    Integer recommendedPriceChangePercent;

    BigDecimal recommendedPrice;

    Long estimatedDemand;

    DssProfitBreakdownResponse targetSituation;

    BigDecimal profitGapVnd;

    Boolean achievable;

    String recommendation;

    String recommendationReason;

    String methodology;
}

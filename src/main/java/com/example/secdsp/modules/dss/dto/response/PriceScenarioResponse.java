package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceScenarioResponse {

    Integer priceChangePercent;

    BigDecimal cost;

    BigDecimal newPrice;

    BigDecimal profitPerProduct;

    Long predictedDemand;

    BigDecimal expectedProfit;

    /** Doanh thu kỳ vọng = giá mới × số lượng dự báo. */
    BigDecimal expectedRevenue;

    /** % thay đổi lợi nhuận ròng so với kịch bản giá hiện tại. */
    BigDecimal profitChangePercent;

    DssProfitBreakdownResponse profitBreakdown;

    String scenarioLabel;

    Boolean recommended;
}

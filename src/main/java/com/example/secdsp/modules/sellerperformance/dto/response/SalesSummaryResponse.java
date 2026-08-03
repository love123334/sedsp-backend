package com.example.secdsp.modules.sellerperformance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Schema(
    description = "Summary of seller sales performance."
)
@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesSummaryResponse {

    @Schema(
        description = "Total revenue from completed orders.",
        example = "15800000"
    )
    BigDecimal totalRevenue;

    @Schema(
        description = "Number of completed orders.",
        example = "245"
    )
    Long completedOrders;

    @Schema(
        description = "Average order value.",
        example = "645000"
    )
    BigDecimal averageOrderValue;
}

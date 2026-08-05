package com.example.secdsp.modules.sellerdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(
    description = "Revenue statistics for the seller."
)
public record RevenueSummary(
    @Schema(
        description = "Total completed revenue.",
        example = "15800000"
    )
    BigDecimal totalRevenue,

    @Schema(
        description = "Number of completed orders.",
        example = "245"
    )
    Long completedOrders,

    @Schema(
        description = "Revenue growth compared to the previous period (percentage).",
        example = "18.5"
    )
    Double growthRate
) {
}
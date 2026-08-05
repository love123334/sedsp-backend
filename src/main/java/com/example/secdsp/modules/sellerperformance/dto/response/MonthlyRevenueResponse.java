package com.example.secdsp.modules.sellerperformance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Schema(
    description = "Monthly revenue information."
)
@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyRevenueResponse {

    @Schema(
        description = "Month in YYYY-MM format.",
        example = "2026-07"
    )
    String month;

    @Schema(
        description = "Revenue generated during the month.",
        example = "3250000"
    )
    BigDecimal revenue;
}
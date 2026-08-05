package com.example.secdsp.modules.sellerdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(
    description = "Order statistics grouped by processing status."
)
public record OrderSummary(
    @Schema(description = "Number of pending orders.", example = "12")
    long pending,

    @Schema(description = "Number of processing orders.", example = "8")
    long processing,

    @Schema(description = "Number of shipping orders.", example = "15")
    long shipping,

    @Schema(description = "Number of delivered orders.", example = "320")
    long delivered
) {
}
package com.example.secdsp.modules.sellerdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(
    description = "Inventory statistics."
)
public record InventorySummary(

    @Schema(description = "Number of low-stock products.", example = "6")
    long lowStockProducts,

    @Schema(description = "Number of out-of-stock products.", example = "2")
    long outOfStockProducts

) {
}
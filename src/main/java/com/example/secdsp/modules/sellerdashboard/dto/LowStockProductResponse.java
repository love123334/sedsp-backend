package com.example.secdsp.modules.sellerdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(
    description = "Product with low inventory."
)
public record LowStockProductResponse(

    @Schema(description = "Product ID.", example = "21")
    Long productId,

    @Schema(description = "Product name.", example = "Wireless Mouse")
    String productName,

    @Schema(description = "Remaining stock quantity.", example = "3")
    Integer quantity
) {
}
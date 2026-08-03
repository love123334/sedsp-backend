package com.example.secdsp.modules.sellerdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(
    description = "Product statistics."
)
public record ProductSummary(

    @Schema(description = "Total number of products.", example = "120")
    long totalProducts,

    @Schema(description = "Number of active products.", example = "98")
    long activeProducts
) {
}
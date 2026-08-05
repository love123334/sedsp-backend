package com.example.secdsp.modules.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Product price history")
public class PriceHistoryResponse {

    @Schema(description = "History identifier", example = "21")
    Long id;

    @Schema(description = "Previous price", example = "27990000")
    BigDecimal oldPrice;

    @Schema(description = "Updated price", example = "29990000")
    BigDecimal newPrice;

    @Schema(
        description = "Price change timestamp",
        example = "2026-08-03T10:20:00Z"
    )
    OffsetDateTime changedAt;
}
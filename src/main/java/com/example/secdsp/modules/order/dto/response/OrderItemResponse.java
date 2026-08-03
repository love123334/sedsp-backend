package com.example.secdsp.modules.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Ordered product")
public class OrderItemResponse {

    @Schema(example = "101")
    Long productId;

    @Schema(example = "Nike Air Force 1")
    String productName;

    @Schema(example = "2")
    Integer quantity;

    @Schema(example = "2500000")
    BigDecimal unitPrice;

    @Schema(example = "5000000")
    BigDecimal subtotal;
}

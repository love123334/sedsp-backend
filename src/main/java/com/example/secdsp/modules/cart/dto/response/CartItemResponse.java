package com.example.secdsp.modules.cart.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Shopping cart item")
public class CartItemResponse {

    @Schema(description = "Cart item identifier", example = "1")
    Long id;

    @Schema(
        description = "Product identifier",
        example = "101"
    )
    Long productId;

    @Schema(
        description = "Product name",
        example = "Nike Air Force 1"
    )
    String productName;

    @Schema(description = "Primary product image — same source as shop listing")
    String productImageUrl;

    @Schema(
        description = "Unit price",
        example = "129.99"
    )
    BigDecimal price;

    @Schema(
        description = "Quantity",
        example = "2"
    )
    Integer quantity;

    @Schema(
        description = "Total price for this item",
        example = "259.98"
    )
    BigDecimal totalPrice;
}
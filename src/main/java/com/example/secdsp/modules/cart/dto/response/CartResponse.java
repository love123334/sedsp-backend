package com.example.secdsp.modules.cart.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Shopping cart information")
public class CartResponse {

    @Schema(
        description = "Cart identifier",
        example = "1"
    )
    Long cartId;

    @Schema(
        description = "Owner user identifier",
        example = "25"
    )
    Long userId;

    @Schema(description = "List of cart items")
    List<CartItemResponse> items;

    @Schema(
        description = "Total cart amount",
        example = "1499.98"
    )
    BigDecimal totalAmount;
}
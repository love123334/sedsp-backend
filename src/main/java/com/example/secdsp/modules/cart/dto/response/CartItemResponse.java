package com.example.secdsp.modules.cart.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemResponse {

    Long id;
    Long productId;
    String productName;
    /** Primary product image — same source as shop listing */
    String productImageUrl;
    BigDecimal price;
    Integer quantity;
    BigDecimal totalPrice;
}
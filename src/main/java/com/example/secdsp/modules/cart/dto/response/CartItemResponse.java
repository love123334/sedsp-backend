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
    BigDecimal price;
    Integer quantity;
    BigDecimal totalPrice;
}
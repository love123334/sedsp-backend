package com.example.secdsp.modules.cart.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartResponse {

    Long cartId;
    Long userId;
    List<CartItemResponse> items;
    BigDecimal totalAmount;
}
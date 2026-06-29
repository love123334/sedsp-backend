package com.example.secdsp.modules.order.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemResponse {

    Long productId;
    String productName;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal subtotal;
}

package com.example.secdsp.modules.order.dto.response;

import com.example.secdsp.modules.order.entity.OrderStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {

    Long id;
    OrderStatus status;
    BigDecimal subtotal;
    BigDecimal shippingFee;
    BigDecimal discount;
    BigDecimal total;
    OffsetDateTime createdAt;
    List<OrderItemResponse> items;
}

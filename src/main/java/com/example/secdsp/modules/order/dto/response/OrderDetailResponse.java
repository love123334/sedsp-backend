package com.example.secdsp.modules.order.dto.response;

import com.example.secdsp.modules.order.entity.OrderTrackingEvent;
import com.example.secdsp.modules.payment.entity.PaymentMethod;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetailResponse {

    OrderResponse order;
    String shippingAddress;
    PaymentMethod paymentMethod;
    List<OrderTrackingEvent> tracking;
}

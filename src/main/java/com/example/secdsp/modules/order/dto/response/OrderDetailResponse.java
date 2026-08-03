package com.example.secdsp.modules.order.dto.response;

import com.example.secdsp.modules.order.entity.OrderTrackingEvent;
import com.example.secdsp.modules.payment.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Order details")
public class OrderDetailResponse {

    @Schema(description = "Order information")
    OrderResponse order;

    @Schema(
        description = "Shipping address",
        example = "123 Nguyen Hue Street, District 1, Ho Chi Minh City"
    )
    String shippingAddress;

    @Schema(
        description = "Payment method",
        implementation = PaymentMethod.class
    )
    PaymentMethod paymentMethod;

    @Schema(
        description = "Tracking history"
    )
    List<OrderTrackingEvent> tracking;
}

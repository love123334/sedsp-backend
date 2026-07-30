package com.example.secdsp.modules.payment.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentGatewayRequest {

    Long orderId;

    BigDecimal amount;

    String orderInfo;

    String returnUrl;

    String notifyUrl;
}

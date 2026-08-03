package com.example.secdsp.modules.payment.dto.response;

import com.example.secdsp.modules.payment.entity.PaymentMethod;
import com.example.secdsp.modules.payment.entity.PaymentStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {

    Long id;
    Long orderId;
    PaymentMethod paymentMethod;
    BigDecimal amount;
    PaymentStatus status;
    String transactionId;
    String currency;
    OffsetDateTime paidAt;
    String redirectUrl;
}
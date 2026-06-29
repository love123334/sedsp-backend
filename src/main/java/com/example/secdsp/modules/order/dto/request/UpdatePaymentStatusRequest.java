package com.example.secdsp.modules.order.dto.request;

import com.example.secdsp.modules.order.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdatePaymentStatusRequest {

    @NotNull
    PaymentStatus status;

    String transactionId;

    String gatewayResponse;
}
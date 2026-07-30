package com.example.secdsp.modules.payment.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentGatewayResponse {

     String redirectUrl;

     String transactionRef;
}

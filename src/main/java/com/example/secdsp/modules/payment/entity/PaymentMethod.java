package com.example.secdsp.modules.payment.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment method")
public enum PaymentMethod {
    COD,
    MOMO,
    MOMO_QR,
    VNPAY
}

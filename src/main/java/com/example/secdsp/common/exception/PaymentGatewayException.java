package com.example.secdsp.common.exception;

import lombok.Getter;

/**
 * Exception thrown when payment gateway operations fail.
 */
@Getter
public class PaymentGatewayException extends BusinessException {

    public PaymentGatewayException(String message) {
        super(ErrorCode.PAYMENT_GATEWAY_ERROR, message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(ErrorCode.PAYMENT_GATEWAY_ERROR, message);
        this.initCause(cause);
    }
}

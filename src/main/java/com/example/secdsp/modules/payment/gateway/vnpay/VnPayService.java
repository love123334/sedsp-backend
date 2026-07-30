package com.example.secdsp.modules.payment.gateway.vnpay;

import com.example.secdsp.modules.payment.dto.request.PaymentGatewayRequest;
import com.example.secdsp.modules.payment.dto.response.PaymentGatewayResponse;

import java.util.Map;

public interface VnPayService {

    PaymentGatewayResponse createPayment(
        PaymentGatewayRequest request
    );

    boolean verifyCallback(
        Map<String, String> params
    );
}

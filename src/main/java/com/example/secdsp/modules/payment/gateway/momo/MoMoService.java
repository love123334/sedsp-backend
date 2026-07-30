package com.example.secdsp.modules.payment.gateway.momo;

import com.example.secdsp.modules.payment.dto.request.PaymentGatewayRequest;
import com.example.secdsp.modules.payment.dto.response.PaymentGatewayResponse;

import java.util.Map;

public interface MoMoService {

    PaymentGatewayResponse createPayment(
        PaymentGatewayRequest request
    );

    boolean verifyCallback(
        Map<String, String> payload
    );
}

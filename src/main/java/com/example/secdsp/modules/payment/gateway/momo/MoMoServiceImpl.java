package com.example.secdsp.modules.payment.gateway.momo;

import com.example.secdsp.common.exception.PaymentGatewayException;
import com.example.secdsp.config.MoMoProperties;
import com.example.secdsp.modules.payment.dto.request.PaymentGatewayRequest;
import com.example.secdsp.modules.payment.dto.response.PaymentGatewayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoMoServiceImpl implements MoMoService {

    private final MoMoProperties properties;
    private final RestTemplate restTemplate;

    @Override
    public PaymentGatewayResponse createPayment(
        PaymentGatewayRequest request
    ) {

        String requestId = UUID.randomUUID().toString();
        String orderId = String.valueOf(request.getOrderId());

        String rawHash =
            "accessKey=" + properties.getAccessKey()
                + "&amount=" + request.getAmount().toPlainString()
                + "&extraData="
                + "&ipnUrl=" + properties.getNotifyUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + request.getOrderInfo()
                + "&partnerCode=" + properties.getPartnerCode()
                + "&redirectUrl=" + properties.getReturnUrl()
                + "&requestId=" + requestId
                + "&requestType=captureWallet";

        String signature =
            hmacSHA256(properties.getSecretKey(), rawHash);

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", properties.getPartnerCode());
        body.put("accessKey", properties.getAccessKey());
        body.put("requestId", requestId);
        body.put("amount", request.getAmount().toPlainString());
        body.put("orderId", orderId);
        body.put("orderInfo", request.getOrderInfo());
        body.put("redirectUrl", properties.getReturnUrl());
        body.put("ipnUrl", properties.getNotifyUrl());
        body.put("requestType", "captureWallet");
        body.put("signature", signature);
        body.put("extraData", "");

        ResponseEntity<Map> response =
            restTemplate.postForEntity(
                properties.getEndpoint(),
                body,
                Map.class
            );

        Map<String, Object> result = response.getBody();

        String payUrl = (String) result.get("payUrl");

        return PaymentGatewayResponse.builder()
            .redirectUrl(payUrl)
            .transactionRef(orderId)
            .build();
    }

    @Override
    public boolean verifyCallback(
        Map<String, String> payload
    ) {

        String receivedSignature = payload.get("signature");

        String rawHash =
            "accessKey=" + properties.getAccessKey()
                + "&amount=" + payload.get("amount")
                + "&extraData=" + payload.get("extraData")
                + "&message=" + payload.get("message")
                + "&orderId=" + payload.get("orderId")
                + "&orderInfo=" + payload.get("orderInfo")
                + "&orderType=" + payload.get("orderType")
                + "&partnerCode=" + payload.get("partnerCode")
                + "&payType=" + payload.get("payType")
                + "&requestId=" + payload.get("requestId")
                + "&responseTime=" + payload.get("responseTime")
                + "&resultCode=" + payload.get("resultCode")
                + "&transId=" + payload.get("transId");

        String calculated =
            hmacSHA256(properties.getSecretKey(), rawHash);

        return calculated.equals(receivedSignature);
    }

    private String hmacSHA256(String key, String data) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec =
                new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
                );

            mac.init(secretKeySpec);

            byte[] hash =
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new PaymentGatewayException("MoMo hash error", e);
        }
    }
}

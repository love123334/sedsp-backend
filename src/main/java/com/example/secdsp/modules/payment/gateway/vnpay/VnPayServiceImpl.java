package com.example.secdsp.modules.payment.gateway.vnpay;

import com.example.secdsp.config.VnPayProperties;
import com.example.secdsp.modules.payment.dto.request.PaymentGatewayRequest;
import com.example.secdsp.modules.payment.dto.response.PaymentGatewayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VnPayServiceImpl implements VnPayService {

    private final VnPayProperties properties;

    @Override
    public PaymentGatewayResponse createPayment(
        PaymentGatewayRequest request
    ) {

        String txnRef = String.valueOf(System.currentTimeMillis());

        Map<String, String> params = new TreeMap<>();

        params.put("vnp_Version", properties.getVersion());
        params.put("vnp_Command", properties.getCommand());
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount",
                   request.getAmount()
                       .multiply(BigDecimal.valueOf(100))
                       .toBigInteger()
                       .toString()
        );
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", request.getOrderInfo());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");

        String queryUrl = buildQuery(params);
        String secureHash = hmacSHA512(properties.getSecretKey(), queryUrl);

        String paymentUrl = properties.getPayUrl()
            + "?"
            + queryUrl
            + "&vnp_SecureHash="
            + secureHash;

        return PaymentGatewayResponse.builder()
            .redirectUrl(paymentUrl)
            .transactionRef(txnRef)
            .build();
    }

    @Override
    public boolean verifyCallback(
        Map<String, String> params
    ) {

        String receivedHash = params.remove("vnp_SecureHash");

        String signData = buildQuery(new TreeMap<>(params));

        String calculatedHash =
            hmacSHA512(properties.getSecretKey(), signData);

        return calculatedHash.equals(receivedHash);
    }

    private String buildQuery(Map<String, String> params) {

        return params.entrySet().stream()
            .map(entry ->
                     entry.getKey()
                         + "="
                         + URLEncoder.encode(
                         entry.getValue(),
                         StandardCharsets.UTF_8
                     )
            )
            .collect(Collectors.joining("&"));
    }

    private String hmacSHA512(
        String key,
        String data
    ) {

        try {

            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec =
                new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
                );

            mac.init(secretKeySpec);

            byte[] hashBytes =
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error generating VNPay hash", e);
        }
    }
}

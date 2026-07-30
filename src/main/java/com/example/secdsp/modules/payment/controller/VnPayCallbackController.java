package com.example.secdsp.modules.payment.controller;

import com.example.secdsp.modules.payment.entity.Payment;
import com.example.secdsp.modules.payment.entity.PaymentStatus;
import com.example.secdsp.modules.payment.gateway.vnpay.VnPayService;
import com.example.secdsp.modules.payment.repository.PaymentRepository;
import com.example.secdsp.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class VnPayCallbackController {

    private final VnPayService vnPayService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> handleReturn(
        @RequestParam Map<String, String> params
    ) {
        Map<String, String> verifyParams = new HashMap<>(params);
        boolean valid = vnPayService.verifyCallback(verifyParams);

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        boolean success = valid && "00".equals(responseCode);

        if (valid && txnRef != null) {
            paymentService.updatePaymentStatusByTxnRef(
                txnRef,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED
            );
        }

        String orderId = paymentRepository.findByTransactionId(txnRef)
            .map(Payment::getOrder)
            .map(o -> String.valueOf(o.getId()))
            .orElse("");

        String target = frontendBaseUrl.replaceAll("/$", "")
            + "/payment/result?gateway=vnpay&orderId="
            + orderId
            + "&status=" + (success ? "success" : "failed");

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, target)
            .build();
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> handleIpn(
        @RequestParam Map<String, String> params
    ) {
        Map<String, String> verifyParams = new HashMap<>(params);
        boolean valid = vnPayService.verifyCallback(verifyParams);

        if (!valid) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        if (txnRef != null) {
            paymentService.updatePaymentStatusByTxnRef(
                txnRef,
                "00".equals(responseCode) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED
            );
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }
}

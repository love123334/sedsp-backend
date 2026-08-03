package com.example.secdsp.modules.payment.controller;

import com.example.secdsp.modules.payment.entity.PaymentStatus;
import com.example.secdsp.modules.payment.gateway.vnpay.VnPayService;
import com.example.secdsp.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class VnPayCallbackController {

    private final VnPayService vnPayService;
    private final PaymentService paymentService;

    @GetMapping("/vnpay-return")
    public ResponseEntity<String> handleReturn(
        @RequestParam Map<String, String> params
    ) {

        boolean valid = vnPayService.verifyCallback(params);

        if (!valid) {
            return ResponseEntity.badRequest()
                .body("Invalid signature");
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        if ("00".equals(responseCode)) {

            paymentService.updatePaymentStatusByTxnRef(
                txnRef,
                PaymentStatus.SUCCESS
            );

        } else {

            paymentService.updatePaymentStatusByTxnRef(
                txnRef,
                PaymentStatus.FAILED
            );
        }

        return ResponseEntity.ok("Payment processed");
    }
}
package com.example.secdsp.modules.payment.controller;

import com.example.secdsp.modules.payment.entity.PaymentStatus;
import com.example.secdsp.modules.payment.gateway.momo.MoMoService;
import com.example.secdsp.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class MoMoCallbackController {

    private final MoMoService moMoService;
    private final PaymentService paymentService;

    @PostMapping("/momo-ipn")
    public ResponseEntity<String> handleMoMoCallback(
        @RequestBody Map<String, String> payload
    ) {

        boolean valid =
            moMoService.verifyCallback(payload);

        if (!valid) {
            return ResponseEntity.badRequest()
                .body("Invalid signature");
        }

        String orderId = payload.get("orderId");
        String resultCode = payload.get("resultCode");

        if ("0".equals(resultCode)) {

            paymentService.updatePaymentStatusByTxnRef(
                orderId,
                PaymentStatus.SUCCESS
            );

        } else {

            paymentService.updatePaymentStatusByTxnRef(
                orderId,
                PaymentStatus.FAILED
            );
        }

        return ResponseEntity.ok("Processed");
    }
}

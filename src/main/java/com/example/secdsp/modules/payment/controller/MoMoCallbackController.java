package com.example.secdsp.modules.payment.controller;

import com.example.secdsp.modules.payment.entity.PaymentStatus;
import com.example.secdsp.modules.payment.gateway.momo.MoMoService;
import com.example.secdsp.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class MoMoCallbackController {

    private final MoMoService moMoService;
    private final PaymentService paymentService;

    @Value("${app.frontend.base-url:https://smartecon-fe.vercel.app}")
    private String frontendBaseUrl;

    @PostMapping("/momo-ipn")
    public ResponseEntity<String> handleMoMoCallback(
        @RequestBody Map<String, String> payload
    ) {

        boolean valid = moMoService.verifyCallback(payload);

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

    /** Browser return URL from MoMo — update status then redirect to FE */
    @GetMapping("/momo-return")
    public ResponseEntity<Void> handleMoMoReturn(
        @RequestParam Map<String, String> payload
    ) {
        String orderId = payload.get("orderId");
        String resultCode = payload.get("resultCode");
        boolean success = "0".equals(resultCode);

        // Return URL may not always include full IPN signature fields; still sync status for UX.
        // IPN remains the authoritative confirmation.
        if (orderId != null && resultCode != null) {
            try {
                paymentService.updatePaymentStatusByTxnRef(
                    orderId,
                    success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED
                );
            } catch (Exception ignored) {
                // order/payment may already be updated by IPN
            }
        }

        String target = frontendBaseUrl.replaceAll("/$", "")
            + "/payment/result?status=" + (success ? "success" : "failed")
            + "&gateway=momo"
            + "&orderId=" + (orderId != null ? orderId : "");

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, target)
            .build();
    }
}

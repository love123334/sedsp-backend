package com.example.secdsp.modules.payment.controller;

import com.example.secdsp.modules.payment.entity.Payment;
import com.example.secdsp.modules.payment.entity.PaymentStatus;
import com.example.secdsp.modules.payment.gateway.vnpay.VnPayService;
import com.example.secdsp.modules.payment.repository.PaymentRepository;
import com.example.secdsp.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class VnPayCallbackController {

    private final VnPayService vnPayService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Value("${app.frontend.base-url:https://smartecon-fe.vercel.app}")
    private String frontendBaseUrl;

    /**
     * Browser return after VNPay (QR / ATM / card). Always redirect to FE cart
     * with pay=success|failed|cancelled banner. Cancel (24) keeps order PENDING.
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> handleReturn(
        @RequestParam Map<String, String> params
    ) {
        Map<String, String> verifyParams = new HashMap<>(params);
        boolean valid = vnPayService.verifyCallback(verifyParams);

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        boolean success = valid && "00".equals(responseCode);
        boolean customerCancel = "24".equals(responseCode);

        if (valid && txnRef != null) {
            try {
                if (success) {
                    paymentService.updatePaymentStatusByTxnRef(txnRef, PaymentStatus.SUCCESS);
                } else if (!customerCancel) {
                    // Definitive gateway failure — cancel order
                    paymentService.updatePaymentStatusByTxnRef(txnRef, PaymentStatus.FAILED);
                } else {
                    log.info("VNPay return: customer cancelled txnRef={} — keep PENDING", txnRef);
                }
            } catch (Exception e) {
                log.error("VNPay return status update failed txnRef={}: {}", txnRef, e.getMessage());
            }
        } else if (!valid) {
            log.warn("VNPay return: invalid signature txnRef={}", txnRef);
        }

        String orderId = "";
        if (txnRef != null) {
            orderId = paymentRepository.findByTransactionId(txnRef)
                .map(Payment::getOrder)
                .map(o -> String.valueOf(o.getId()))
                .orElse("");
        }

        String statusParam = success ? "success" : (customerCancel ? "cancelled" : "failed");
        // Public bridge page (no auth) → then FE routes to /cart with banner
        String target = frontendBaseUrl.replaceAll("/$", "")
            + "/payment/result?status=" + statusParam
            + "&gateway=vnpay"
            + "&orderId=" + encode(orderId)
            + "&code=" + encode(responseCode == null ? "" : responseCode)
            + (txnRef != null ? "&txnRef=" + encode(txnRef) : "");

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

        try {
            if (txnRef != null && "00".equals(responseCode)) {
                paymentService.updatePaymentStatusByTxnRef(txnRef, PaymentStatus.SUCCESS);
            } else if (txnRef != null && !"24".equals(responseCode)) {
                paymentService.updatePaymentStatusByTxnRef(txnRef, PaymentStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("VNPay IPN update failed txnRef={}: {}", txnRef, e.getMessage());
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Update failed"));
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}

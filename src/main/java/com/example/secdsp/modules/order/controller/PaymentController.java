package com.example.secdsp.modules.order.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.order.dto.request.PayOrderRequest;
import com.example.secdsp.modules.order.dto.request.UpdatePaymentStatusRequest;
import com.example.secdsp.modules.order.dto.response.PaymentResponse;
import com.example.secdsp.modules.order.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>>
    payOrder(
        @PathVariable Long orderId,
        @Valid @RequestBody PayOrderRequest request
    ) {

        return ResponseEntity.ok(
            ApiResponse.success(
                "Payment initiated",
                paymentService.payOrder(orderId, request)
            )
        );
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>>
    getPaymentByOrder(@PathVariable Long orderId) {

        return ResponseEntity.ok(
            ApiResponse.success(
                paymentService.getPaymentByOrderId(orderId)
            )
        );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>>
    getMyPayments(
        @PageableDefault(size = 10) Pageable pageable
    ) {

        return ResponseEntity.ok(
            ApiResponse.success(
                paymentService.getMyPayments(pageable)
            )
        );
    }

    @PutMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>>
    updatePaymentStatus(
        @PathVariable Long paymentId,
        @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {

        return ResponseEntity.ok(
            ApiResponse.success(
                "Payment status updated",
                paymentService.updatePaymentStatus(paymentId, request)
            )
        );
    }
}
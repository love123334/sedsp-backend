package com.example.secdsp.modules.order.service;

import com.example.secdsp.modules.order.dto.request.PayOrderRequest;
import com.example.secdsp.modules.order.dto.request.UpdatePaymentStatusRequest;
import com.example.secdsp.modules.order.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentResponse getPaymentByOrderId(Long orderId);

    PaymentResponse payOrder(
        Long orderId,
        PayOrderRequest request
    );

    Page<PaymentResponse> getMyPayments(Pageable pageable);

    PaymentResponse updatePaymentStatus(
        Long paymentId,
        UpdatePaymentStatusRequest request
    );


}

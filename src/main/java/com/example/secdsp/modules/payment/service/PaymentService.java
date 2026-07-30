package com.example.secdsp.modules.payment.service;

import com.example.secdsp.modules.order.dto.internal.MonthlyRevenueInfo;
import com.example.secdsp.modules.order.dto.internal.RevenueInfo;
import com.example.secdsp.modules.order.dto.internal.SalesSummaryInfo;
import com.example.secdsp.modules.order.dto.request.PayOrderRequest;
import com.example.secdsp.modules.payment.dto.request.UpdatePaymentStatusRequest;
import com.example.secdsp.modules.payment.dto.response.PaymentResponse;
import com.example.secdsp.modules.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

    RevenueInfo getRevenue(Long sellerId);

    SalesSummaryInfo getSalesSummary(Long sellerId);

    List<MonthlyRevenueInfo> getMonthlyRevenue(Long sellerId);

    void updatePaymentStatusByTxnRef(
        String txnRef,
        PaymentStatus status
    );
}

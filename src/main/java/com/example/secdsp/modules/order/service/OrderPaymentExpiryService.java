package com.example.secdsp.modules.order.service;

import com.example.secdsp.config.OrderProperties;
import com.example.secdsp.modules.inventory.service.InventoryInternalService;
import com.example.secdsp.modules.order.entity.Order;
import com.example.secdsp.modules.order.entity.OrderItem;
import com.example.secdsp.modules.order.entity.OrderStatus;
import com.example.secdsp.modules.order.entity.OrderTracking;
import com.example.secdsp.modules.order.entity.OrderTrackingEvent;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.order.repository.OrderRepository;
import com.example.secdsp.modules.order.repository.OrderTrackingRepository;
import com.example.secdsp.modules.payment.entity.Payment;
import com.example.secdsp.modules.payment.entity.PaymentMethod;
import com.example.secdsp.modules.payment.entity.PaymentStatus;
import com.example.secdsp.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentExpiryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryInternalService inventoryInternalService;
    private final OrderProperties orderProperties;

    @Scheduled(fixedDelayString = "${app.order.payment-expiry-check-ms:60000}")
    @Transactional
    public void cancelExpiredUnpaidOrders() {
        OffsetDateTime cutoff = OffsetDateTime.now()
            .minusMinutes(Math.max(5, orderProperties.getPaymentTimeoutMinutes()));

        List<Order> expired = orderRepository.findPendingOlderThan(cutoff).stream()
            .filter(this::isUnpaidGatewayOrder)
            .toList();

        for (Order order : expired) {
            cancelExpiredOrder(order);
        }

        if (!expired.isEmpty()) {
            log.info("Auto-cancelled {} unpaid orders older than {} minutes",
                expired.size(), orderProperties.getPaymentTimeoutMinutes());
        }
    }

    private boolean isUnpaidGatewayOrder(Order order) {
        Payment payment = paymentRepository.findByOrder_Id(order.getId()).orElse(null);
        if (payment == null) {
            return true;
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return false;
        }
        PaymentMethod method = payment.getPaymentMethod();
        return method == PaymentMethod.VNPAY || method == PaymentMethod.MOMO;
    }

    private void cancelExpiredOrder(Order order) {
        order.setStatus(OrderStatus.CANCELLED);

        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        for (OrderItem item : items) {
            inventoryInternalService.releaseForCancel(
                item.getProduct().getId(),
                item.getQuantity()
            );
        }

        paymentRepository.findByOrder_Id(order.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
            }
        });

        var tracking = new OrderTracking();
        tracking.setOrder(order);
        tracking.setEvent(OrderTrackingEvent.CANCELLED_BY_ADMIN);
        tracking.setNote("Order auto-cancelled: payment timeout");
        orderTrackingRepository.save(tracking);
    }
}

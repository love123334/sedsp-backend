package com.example.secdsp.modules.order.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.inventory.service.InventoryInternalService;
import com.example.secdsp.modules.order.dto.internal.MonthlyRevenueInfo;
import com.example.secdsp.modules.order.dto.internal.RevenueInfo;
import com.example.secdsp.modules.order.dto.internal.SalesSummaryInfo;
import com.example.secdsp.modules.order.dto.request.PayOrderRequest;
import com.example.secdsp.modules.order.dto.request.UpdatePaymentStatusRequest;
import com.example.secdsp.modules.order.dto.response.PaymentResponse;
import com.example.secdsp.modules.order.entity.*;
import com.example.secdsp.modules.order.mapper.PaymentMapper;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.order.repository.OrderRepository;
import com.example.secdsp.modules.order.repository.OrderTrackingRepository;
import com.example.secdsp.modules.order.repository.PaymentRepository;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final PaymentMapper paymentMapper;
    private final InventoryInternalService inventoryInternalService;
    private final OrderNotificationService orderNotificationService;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Payment", orderId));

        if (!payment.getOrder().getUser().getId().equals(userId)
            && !SecurityUtils.hasRole(UserRole.ADMIN)) {

            throw new UnauthorizedException(
                "You are not allowed to view this payment."
            );
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse payOrder(
        Long orderId,
        PayOrderRequest request
    ) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException(
                "You cannot pay for this order."
            );
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Payment", orderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException("Order already paid.");
        }

        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);

        // TODO: Call payment gateway here (VNPay, MoMo, etc.)
        // After gateway callback → updatePaymentStatus()

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Pageable pageable) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Page<Payment> payments =
            paymentRepository.findByOrder_User_Id(userId, pageable);

        return payments.map(paymentMapper::toResponse);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(
        Long paymentId,
        UpdatePaymentStatusRequest request
    ) {

        if (!SecurityUtils.hasRole(UserRole.ADMIN)) {
            throw new UnauthorizedException(
                "Only admin can update payment status."
            );
        }

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Payment", paymentId));

        if (request.getTransactionId() != null) {

            if (paymentRepository.existsByTransactionId(
                request.getTransactionId())) {

                throw new BusinessException(
                    "Duplicate transaction ID."
                );
            }

            payment.setTransactionId(request.getTransactionId());
        }

        payment.setGatewayResponse(request.getGatewayResponse());
        payment.setStatus(request.getStatus());

        if (request.getStatus() == PaymentStatus.SUCCESS) {

            payment.setPaidAt(LocalDateTime.now());

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.PAID);

            insertTracking(order,
                           OrderTrackingEvent.PAYMENT_SUCCESS);

            orderNotificationService.notifyStatusChanged(order, OrderStatus.PAID);

        } else if (request.getStatus() == PaymentStatus.FAILED) {

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.CANCELLED);

            List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
            for (OrderItem item : items) {
                inventoryInternalService.releaseForCancel(
                    item.getProduct().getId(),
                    item.getQuantity()
                );
            }

            insertTracking(order,
                           OrderTrackingEvent.PAYMENT_FAILED);

            orderNotificationService.notifyCancelled(order, "Thanh toan that bai — don da huy.");
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueInfo getRevenue(Long sellerId) {

        BigDecimal totalRevenue =
            orderItemRepository
                .calculateSellerRevenue(sellerId);

        long completedOrders =
            orderItemRepository
                .countCompletedOrdersBySeller(sellerId);

        return RevenueInfo.builder()
            .totalRevenue(
                totalRevenue != null
                    ? totalRevenue
                    : BigDecimal.ZERO
            )
            .completedOrders(completedOrders)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesSummaryInfo getSalesSummary(Long sellerId) {

        BigDecimal totalRevenue =
            orderItemRepository.calculateSellerRevenue(sellerId);

        Long completedOrders =
            orderItemRepository.countCompletedOrdersBySeller(sellerId);

        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        BigDecimal averageOrderValue =
            completedOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(
                BigDecimal.valueOf(completedOrders),
                0,
                RoundingMode.HALF_UP
            );

        return SalesSummaryInfo.builder()
            .totalRevenue(totalRevenue)
            .completedOrders(completedOrders)
            .averageOrderValue(averageOrderValue)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyRevenueInfo> getMonthlyRevenue(Long sellerId) {

        List<Object[]> results =
            orderItemRepository.calculateMonthlyRevenue(sellerId);

        return results.stream()
            .map(row -> {

                String month = (String) row[0];
                BigDecimal revenue =
                    row[1] != null
                        ? (BigDecimal) row[1]
                        : BigDecimal.ZERO;

                return MonthlyRevenueInfo.builder()
                    .month(month)
                    .revenue(revenue)
                    .build();
            })
            .toList();
    }
    private void insertTracking(
        Order order,
        OrderTrackingEvent event
    ) {

        OrderTracking tracking = new OrderTracking();
        tracking.setOrder(order);
        tracking.setEvent(event);

        User admin = new User();
        admin.setId(SecurityUtils.getCurrentUserId());

        tracking.setUpdatedBy(admin);

        orderTrackingRepository.save(tracking);
    }
}

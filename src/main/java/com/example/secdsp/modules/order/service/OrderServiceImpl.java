package com.example.secdsp.modules.order.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.cart.entity.Cart;
import com.example.secdsp.modules.cart.entity.CartItem;
import com.example.secdsp.modules.cart.repository.CartItemRepository;
import com.example.secdsp.modules.cart.repository.CartRepository;
import com.example.secdsp.modules.cart.service.CartService;
import com.example.secdsp.modules.inventory.service.InventoryInternalService;
import com.example.secdsp.modules.order.dto.request.CreateOrderRequest;
import com.example.secdsp.modules.order.dto.response.OrderDetailResponse;
import com.example.secdsp.modules.order.dto.response.OrderItemResponse;
import com.example.secdsp.modules.order.dto.response.OrderResponse;
import com.example.secdsp.modules.order.entity.*;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.order.repository.OrderRepository;
import com.example.secdsp.modules.order.repository.OrderTrackingRepository;
import com.example.secdsp.modules.order.repository.PaymentRepository;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.entity.ProductStatus;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final PaymentRepository paymentRepository;

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final InventoryInternalService inventoryInternalService;
    private final ProductService productService;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Cart cart = cartRepository.findByUser_Id(userId)
            .orElseThrow(() -> new BusinessException("Cart is empty."));

        List<CartItem> cartItems =
            cartItemRepository.findByCart_Id(cart.getId());

        if (cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cartItems) {

            ProductInfo product =
                productService.getProductInfo(item.getProduct().getId());

            if (product.status() != ProductStatus.ACTIVE) {
                throw new BusinessException(
                    "Product is no longer available."
                );
            }

            if (product.price() == null) {
                throw new BusinessException("Invalid product price.");
            }

            subtotal = subtotal.add(
                product.price().multiply(
                    BigDecimal.valueOf(item.getQuantity())
                )
            );
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total =
            subtotal.add(shippingFee).subtract(discount);

        Order order = new Order();

        User userRef = new User();
        userRef.setId(userId);

        order.setUser(userRef);
        order.setSubtotalAmount(subtotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());

        orderRepository.save(order);

        for (CartItem item : cartItems) {

            ProductInfo product =
                productService.getProductInfo(item.getProduct().getId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);

            Product productRef = new Product();
            productRef.setId(product.id());

            orderItem.setProduct(productRef);
            orderItem.setProductNameAtPurchase(product.name());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPriceAtPurchase(product.price());

            BigDecimal itemSubtotal =
                product.price().multiply(
                    BigDecimal.valueOf(item.getQuantity())
                );

            orderItem.setSubtotal(itemSubtotal);

            orderItemRepository.save(orderItem);

            // ✅ Reserve inventory (available--, reserved++)
            inventoryInternalService.reserveForOrder(
                product.id(),
                item.getQuantity()
            );
        }

        // Tracking CREATED
        OrderTracking tracking = new OrderTracking();
        tracking.setOrder(order);
        tracking.setEvent(OrderTrackingEvent.CREATED);
        tracking.setNote("Order created.");

        User updatedBy = new User();
        updatedBy.setId(userId);
        tracking.setUpdatedBy(updatedBy);

        orderTrackingRepository.save(tracking);

        // Payment
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(total);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCurrency("VND");

        paymentRepository.save(payment);

        // TODO: Integrate payment gateway here (VNPay, MoMo, etc.)

        cartItemRepository.deleteAllByCart_Id(cart.getId());

        return buildOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(Long id) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Order order = orderRepository.findById(id)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Order", id));

        if (!order.getUser().getId().equals(userId)
            && !SecurityUtils.hasRole(UserRole.ADMIN)) {
            throw new UnauthorizedException(
                "You are not allowed to view this order."
            );
        }

        OrderResponse orderResponse = buildOrderResponse(order);

        List<OrderTracking> trackingList =
            orderTrackingRepository
                .findByOrder_IdOrderByCreatedAtDesc(id);

        List<OrderTrackingEvent> trackingEvents =
            trackingList.stream()
                .map(OrderTracking::getEvent)
                .toList();

        Payment payment =
            paymentRepository.findByOrder_Id(id)
                .orElse(null);

        return OrderDetailResponse.builder()
            .order(orderResponse)
            .shippingAddress(order.getShippingAddress())
            .paymentMethod(
                payment != null
                    ? payment.getPaymentMethod()
                    : null
            )
            .tracking(trackingEvents)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Pageable pageable) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Page<Order> orders =
            orderRepository.findByUser_Id(userId, pageable);

        return orders.map(this::buildOrderResponse);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Order order = orderRepository.findById(id)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Order", id));

        if (!order.getUser().getId().equals(userId)
            && !SecurityUtils.hasRole(UserRole.ADMIN)) {

            throw new UnauthorizedException(
                "You cannot cancel this order."
            );
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(
                "Only pending orders can be cancelled."
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        List<OrderItem> items =
            orderItemRepository.findByOrder_Id(order.getId());

        for (OrderItem item : items) {

            inventoryInternalService.releaseForCancel(
                item.getProduct().getId(),
                item.getQuantity()
            );
        }

        OrderTracking tracking = new OrderTracking();
        tracking.setOrder(order);
        tracking.setEvent(OrderTrackingEvent.CANCELLED_BY_USER);
        tracking.setNote("Order cancelled.");

        User userRef = new User();
        userRef.setId(userId);
        tracking.setUpdatedBy(userRef);

        orderTrackingRepository.save(tracking);

        // ✅ Update payment status
        Payment payment =
            paymentRepository.findByOrder_Id(order.getId())
                .orElse(null);

        if (payment != null) {
            payment.setStatus(PaymentStatus.FAILED);
        }
    }


    private OrderResponse buildOrderResponse(Order order) {

        List<OrderItem> items =
            orderItemRepository.findByOrder_Id(order.getId());

        List<OrderItemResponse> itemResponses =
            items.stream().map(item ->
                                   OrderItemResponse.builder()
                                       .productId(item.getProduct().getId())
                                       .productName(item.getProductNameAtPurchase())
                                       .quantity(item.getQuantity())
                                       .unitPrice(item.getUnitPriceAtPurchase())
                                       .subtotal(item.getSubtotal())
                                       .build()
            ).toList();

        return OrderResponse.builder()
            .id(order.getId())
            .status(order.getStatus())
            .subtotal(order.getSubtotalAmount())
            .shippingFee(order.getShippingFee())
            .discount(order.getDiscountAmount())
            .total(order.getTotalAmount())
            .createdAt(order.getCreatedAt())
            .items(itemResponses)
            .build();
    }
}

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
import com.example.secdsp.modules.order.dto.internal.OrderDashboardInfo;
import com.example.secdsp.modules.order.dto.internal.RecentOrderInfo;
import com.example.secdsp.modules.order.dto.internal.TopProductSalesInfo;
import com.example.secdsp.modules.order.dto.request.CreateOrderRequest;
import com.example.secdsp.modules.order.dto.request.UpdateOrderStatusRequest;
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
import java.util.ArrayList;
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
    private final OrderNotificationService orderNotificationService;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        Long userId = requireCurrentUserId();

        Cart cart = getUserCartOrThrow(userId);

        List<CartItem> cartItems = getCartItemsOrThrow(cart);

        // 1. Tính tổng tiền & Kiểm tra trạng thái/giá sản phẩm
        BigDecimal subtotal = calculateSubtotal(cartItems);

        // 2. Tạo đơn hàng PENDING
        Order order = createOrderEntity(userId, request, subtotal);

        // 3. Tạo các OrderItem (Snapshot Giá & Tên) + Giữ chỗ tồn kho Atomically
        createOrderItemsAndReserveInventory(order, cartItems);

        // 4. Tạo lịch sử theo dõi (Order Tracking)
        createTracking(order, userId);

        // 5. Tạo thông tin thanh toán (Payment)
        createPayment(order, request.getPaymentMethod());

        // 6. Xóa giỏ hàng
        clearCart(cart);

        orderNotificationService.notifyOrderCreated(order);

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
            && !SecurityUtils.hasRole(UserRole.ADMIN)
            && !SecurityUtils.hasRole(UserRole.MANAGER)) {

            boolean sellerOwns = orderItemRepository.findByOrder_Id(id).stream()
                .anyMatch(i -> i.getSeller() != null && userId.equals(i.getSeller().getId()));
            if (!sellerOwns) {
                throw new UnauthorizedException(
                    "You are not allowed to view this order."
                );
            }
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
    @Transactional(readOnly = true)
    public Page<OrderResponse> getSellerOrders(Pageable pageable) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        if (sellerId == null) {
            throw new UnauthorizedException("Authentication required.");
        }
        if (SecurityUtils.hasRole(UserRole.ADMIN)
            || SecurityUtils.hasRole(UserRole.MANAGER)) {
            return orderRepository.findAll(pageable).map(this::buildOrderResponse);
        }
        if (!SecurityUtils.hasRole(UserRole.SELLER)) {
            throw new UnauthorizedException("Only sellers can view seller orders.");
        }
        return orderRepository
            .findDistinctBySellerId(sellerId, pageable)
            .map(this::buildOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Order order = orderRepository.findWithItemsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        boolean isAdminOrManager =
            SecurityUtils.hasRole(UserRole.ADMIN)
                || SecurityUtils.hasRole(UserRole.MANAGER);
        boolean isSellerOwner = order.getItems().stream()
            .anyMatch(i -> i.getSeller() != null && userId.equals(i.getSeller().getId()));

        if (!isAdminOrManager && !isSellerOwner) {
            throw new UnauthorizedException("You cannot update this order.");
        }

        OrderStatus current = order.getStatus();
        OrderStatus next = request.getStatus();
        if (current == next) {
            return buildOrderResponse(order);
        }

        validateStatusTransition(current, next);

        order.setStatus(next);

        OrderTracking tracking = new OrderTracking();
        tracking.setOrder(order);
        tracking.setEvent(mapTrackingEvent(next));
        tracking.setNote(
            request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote()
                : "Status updated to " + next
        );
        User actor = new User();
        actor.setId(userId);
        tracking.setUpdatedBy(actor);
        orderTrackingRepository.save(tracking);

        if (next == OrderStatus.CANCELLED && current == OrderStatus.PENDING) {
            for (OrderItem item : order.getItems()) {
                inventoryInternalService.releaseForCancel(
                    item.getProduct().getId(),
                    item.getQuantity()
                );
            }
        }

        orderRepository.save(order);
        log.info("Order {} status {} -> {} by user {}", id, current, next, userId);
        if (next == OrderStatus.CANCELLED) {
            orderNotificationService.notifyCancelled(order, "Don hang da bi huy boi shop/quan ly.");
        } else {
            orderNotificationService.notifyStatusChanged(order, next);
        }
        return buildOrderResponse(order);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean ok = switch (current) {
            case PENDING -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PAID -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!ok) {
            throw new BusinessException(
                "Invalid status transition: " + current + " -> " + next
            );
        }
    }

    private OrderTrackingEvent mapTrackingEvent(OrderStatus status) {
        return switch (status) {
            case PROCESSING, PAID -> OrderTrackingEvent.CONFIRMED;
            case SHIPPING -> OrderTrackingEvent.SHIPPED;
            case DELIVERED -> OrderTrackingEvent.DELIVERED;
            case CANCELLED -> OrderTrackingEvent.CANCELLED_BY_ADMIN;
            default -> OrderTrackingEvent.CONFIRMED;
        };
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

        orderNotificationService.notifyCancelled(order, "Don hang da bi huy boi nguoi mua.");
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDashboardInfo getSellerOrderSummary(Long sellerId) {

        long pending =
            orderItemRepository.countBySeller_IdAndOrder_Status(
                sellerId,
                OrderStatus.PENDING
            );

        long processing =
            orderItemRepository.countBySeller_IdAndOrder_Status(
                sellerId,
                OrderStatus.PROCESSING
            );

        long shipping =
            orderItemRepository.countBySeller_IdAndOrder_Status(
                sellerId,
                OrderStatus.SHIPPING
            );

        long delivered =
            orderItemRepository.countBySeller_IdAndOrder_Status(
                sellerId,
                OrderStatus.DELIVERED
            );

        return OrderDashboardInfo.builder()
            .pending(pending)
            .processing(processing)
            .shipping(shipping)
            .delivered(delivered)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentOrderInfo> getRecentOrders(Long sellerId) {

        return orderItemRepository
            .findTop5BySeller_IdOrderByOrder_CreatedAtDesc(sellerId)
            .stream()
            .map(item ->
                     RecentOrderInfo.builder()
                         .orderId(item.getOrder().getId())
                         .customer(
                             item.getOrder()
                                 .getUser()
                                 .getUsername()
                         )
                         .total(item.getOrder().getTotalAmount())
                         .status(item.getOrder().getStatus())
                         .createdAt(item.getOrder().getCreatedAt())
                         .build()
            ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopProductSalesInfo> getTopSellingProducts(Long sellerId) {

        List<Object[]> results =
            orderItemRepository.findTopSellingProducts(sellerId);

        return results.stream()
            .map(row -> TopProductSalesInfo.builder()
                .productId((Long) row[0])
                .productName((String) row[1])
                .quantitySold((Long) row[2])
                .revenue((BigDecimal) row[3])
                .build()
            )
            .toList();
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

    private Long requireCurrentUserId() {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        return userId;
    }

    private Cart getUserCartOrThrow(Long userId) {

        return cartRepository.findByUser_Id(userId)
            .orElseThrow(() ->
                             new BusinessException("Cart is empty."));
    }

    private List<CartItem> getCartItemsOrThrow(Cart cart) {

        List<CartItem> items =
            cartItemRepository.findByCart_Id(cart.getId());

        if (items.isEmpty()) {
            throw new BusinessException("Cart is empty.");
        }

        return items;
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {

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
                throw new BusinessException(
                    "Invalid product price."
                );
            }

            subtotal = subtotal.add(
                product.price().multiply(
                    BigDecimal.valueOf(item.getQuantity())
                )
            );
        }

        return subtotal;
    }

    private Order createOrderEntity(
        Long userId,
        CreateOrderRequest request,
        BigDecimal subtotal
    ) {

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

        return orderRepository.save(order);
    }

    private void createOrderItemsAndReserveInventory(
        Order order,
        List<CartItem> cartItems
    ) {
        List<OrderItem> orderItemsToSave = new ArrayList<>();

        for (CartItem item : cartItems) {

            ProductInfo product = productService.getProductInfo(item.getProduct().getId());

            inventoryInternalService.reserveForOrder(
                product.id(),
                item.getQuantity()
            );

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);

            Product productRef = new Product();
            productRef.setId(product.id());

            orderItem.setProduct(productRef);
            orderItem.setProductNameAtPurchase(product.name());
            orderItem.setUnitPriceAtPurchase(product.price());
            orderItem.setQuantity(item.getQuantity());

            if (product.sellerId() == null) {
                throw new BusinessException(
                    "Product " + product.id() + " has no seller assigned."
                );
            }
            User sellerRef = new User();
            sellerRef.setId(product.sellerId());
            orderItem.setSeller(sellerRef);

            BigDecimal itemSubtotal = product.price().multiply(
                BigDecimal.valueOf(item.getQuantity())
            );
            orderItem.setSubtotal(itemSubtotal);

            orderItemsToSave.add(orderItem);
        }

        orderItemRepository.saveAll(orderItemsToSave);
    }

    private void createTracking(Order order, Long userId) {

        OrderTracking tracking = new OrderTracking();
        tracking.setOrder(order);
        tracking.setEvent(OrderTrackingEvent.CREATED);
        tracking.setNote("Order created.");

        User updatedBy = new User();
        updatedBy.setId(userId);

        tracking.setUpdatedBy(updatedBy);

        orderTrackingRepository.save(tracking);
    }

    private void createPayment(
        Order order,
        PaymentMethod method
    ) {

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(method);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCurrency("VND");

        paymentRepository.save(payment);

        // TODO: Integrate payment gateway here
    }

    private void clearCart(Cart cart) {

        cartItemRepository.hardDeleteAllByCartId(cart.getId());
    }
}

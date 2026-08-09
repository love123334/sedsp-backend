package com.example.secdsp.modules.order.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.util.PublicAssetUrlResolver;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.cart.entity.Cart;
import com.example.secdsp.modules.cart.entity.CartItem;
import com.example.secdsp.modules.cart.repository.CartItemRepository;
import com.example.secdsp.modules.cart.repository.CartRepository;
import com.example.secdsp.modules.inventory.service.InventoryInternalService;
import com.example.secdsp.modules.order.dto.internal.OrderDashboardInfo;
import com.example.secdsp.modules.order.dto.internal.RecentOrderInfo;
import com.example.secdsp.modules.order.dto.internal.TopProductSalesInfo;
import com.example.secdsp.modules.order.dto.request.CreateOrderRequest;
import com.example.secdsp.modules.order.dto.request.UpdateOrderStatusRequest;
import com.example.secdsp.modules.order.dto.response.MomoTransferInfo;
import com.example.secdsp.modules.order.dto.response.OrderDetailResponse;
import com.example.secdsp.modules.order.dto.response.OrderItemResponse;
import com.example.secdsp.modules.order.dto.response.OrderResponse;
import com.example.secdsp.modules.order.entity.*;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.order.repository.OrderRepository;
import com.example.secdsp.modules.order.repository.OrderTrackingRepository;
import com.example.secdsp.modules.payment.repository.PaymentRepository;
import com.example.secdsp.modules.payment.entity.Payment;
import com.example.secdsp.modules.payment.entity.PaymentMethod;
import com.example.secdsp.modules.payment.entity.PaymentStatus;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.entity.ProductStatus;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.order.support.MomoTransferSupport;
import com.example.secdsp.modules.user.repository.UserRepository;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.service.SellerMomoServiceImpl;
import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final PaymentRepository paymentRepository;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final InventoryInternalService inventoryInternalService;
    private final ProductService productService;
    private final OrderNotificationService orderNotificationService;
    private final VoucherService voucherService;
    private final UserRepository userRepository;
    private final PublicAssetUrlResolver publicAssetUrlResolver;

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

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucherService.applyToOrder(order, request.getVoucherCode(), cartItems, userId);
            orderRepository.save(order);
        }

        // 3. Tạo các OrderItem (Snapshot Giá & Tên) + Giữ chỗ tồn kho Atomically
        createOrderItemsAndReserveInventory(order, cartItems);

        if (request.getPaymentMethod() == PaymentMethod.MOMO_QR) {
            validateMomoQrCart(cartItems);
        }

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
            .momoTransfer(buildMomoTransferInfo(order, payment))
            .tracking(trackingEvents)
            .build();
    }

    @Override
    @Transactional
    public OrderResponse confirmMomoTransfer(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        Order order = orderRepository.findWithItemsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        boolean isSellerOwner = order.getItems().stream()
            .anyMatch(i -> i.getSeller() != null && userId.equals(i.getSeller().getId()));
        if (!isSellerOwner
            && !SecurityUtils.hasRole(UserRole.ADMIN)
            && !SecurityUtils.hasRole(UserRole.MANAGER)) {
            throw new UnauthorizedException("You cannot confirm this payment.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Only pending orders can be confirmed.");
        }

        Payment payment = paymentRepository.findByOrder_Id(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", id));

        if (payment.getPaymentMethod() != PaymentMethod.MOMO_QR) {
            throw new BusinessException("Order is not a MoMo QR transfer.");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return buildOrderResponse(order);
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(OffsetDateTime.now());
        order.setStatus(OrderStatus.PAID);

        OrderTracking tracking = new OrderTracking();
        tracking.setOrder(order);
        tracking.setEvent(OrderTrackingEvent.PAYMENT_SUCCESS);
        tracking.setNote("Seller confirmed MoMo transfer received.");
        User actor = new User();
        actor.setId(userId);
        tracking.setUpdatedBy(actor);
        orderTrackingRepository.save(tracking);

        orderRepository.save(order);
        orderNotificationService.notifyStatusChanged(order, OrderStatus.PAID);
        log.info("MoMo QR transfer confirmed for order {} by user {}", id, userId);
        return buildOrderResponse(order);
    }

    private MomoTransferInfo buildMomoTransferInfo(Order order, Payment payment) {
        if (payment == null || payment.getPaymentMethod() != PaymentMethod.MOMO_QR) {
            return null;
        }

        User seller = resolveSingleSeller(order.getId());
        if (seller == null) {
            return MomoTransferInfo.builder()
                .amount(order.getTotalAmount())
                .transferNote(payment.getTransferNote())
                .configured(false)
                .build();
        }

        return MomoTransferInfo.builder()
            .amount(payment.getAmount())
            .transferNote(
                payment.getTransferNote() != null
                    ? payment.getTransferNote()
                    : MomoTransferSupport.transferNote(order.getId())
            )
            .sellerMomoPhone(seller.getMomoPhone())
            .sellerMomoQrUrl(publicAssetUrlResolver.resolve(seller.getMomoQrUrl()))
            .sellerStoreName(seller.getStoreName())
            .configured(SellerMomoServiceImpl.isConfigured(seller))
            .build();
    }

    private User resolveSingleSeller(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        Set<Long> sellerIds = new HashSet<>();
        for (OrderItem item : items) {
            if (item.getSeller() != null) {
                sellerIds.add(item.getSeller().getId());
            }
        }
        if (sellerIds.size() != 1) {
            return null;
        }
        Long sellerId = sellerIds.iterator().next();
        return userRepository.findById(sellerId).orElse(null);
    }

    private void validateMomoQrCart(List<CartItem> cartItems) {
        Long sellerId = null;
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getSeller() == null) {
                throw new BusinessException("Product has no seller.");
            }
            Long currentSellerId = product.getSeller().getId();
            if (sellerId == null) {
                sellerId = currentSellerId;
            } else if (!sellerId.equals(currentSellerId)) {
                throw new BusinessException(
                    "Chuyen MoMo shop chi ap dung khi gio hang chi co san pham tu mot cua hang."
                );
            }
        }
        Long resolvedSellerId = sellerId;
        User seller = userRepository.findById(resolvedSellerId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller", resolvedSellerId));
        if (!SellerMomoServiceImpl.isConfigured(seller)) {
            throw new BusinessException(
                "Cua hang chua cau hinh so MoMo hoac anh QR. Vui long chon phuong thuc khac."
            );
        }
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

        return mapOrderPage(orders);
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
            return mapOrderPage(orderRepository.findAll(pageable));
        }
        if (!SecurityUtils.hasRole(UserRole.SELLER)) {
            throw new UnauthorizedException("Only sellers can view seller orders.");
        }
        return mapOrderPage(
            orderRepository.findDistinctBySellerId(sellerId, pageable)
        );
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

        long pending = orderRepository.countBySellerIdAndStatus(
            sellerId,
            OrderStatus.PENDING
        );

        long processing =
            orderRepository.countBySellerIdAndStatus(sellerId, OrderStatus.PAID)
                + orderRepository.countBySellerIdAndStatus(
                    sellerId,
                    OrderStatus.PROCESSING
                );

        long shipping = orderRepository.countBySellerIdAndStatus(
            sellerId,
            OrderStatus.SHIPPING
        );

        long delivered = orderRepository.countBySellerIdAndStatus(
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

        return orderRepository
            .findDistinctBySellerId(sellerId, PageRequest.of(0, 5))
            .stream()
            .map(order ->
                     RecentOrderInfo.builder()
                         .orderId(order.getId())
                         .customer(order.getUser().getUsername())
                         .total(order.getTotalAmount())
                         .status(order.getStatus())
                         .createdAt(order.getCreatedAt())
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

    @Override
    @Transactional(readOnly = true)
    public LocalDate getFirstCompletedSaleDate(Long productId) {
        return orderItemRepository
            .findFirstCompletedSaleDateByProduct(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompletedQuantitySold(
        Long productId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        return orderItemRepository
            .findCompletedDailySalesByProduct(
                productId,
                startDateTime,
                endDateTime
            )
            .stream()
            .mapToLong(row -> ((Number) row[1]).longValue())
            .sum();
    }

    private Page<OrderResponse> mapOrderPage(Page<Order> orders) {
        List<Order> content = orders.getContent();
        if (content.isEmpty()) {
            return orders.map(order -> buildOrderResponse(order, List.of()));
        }
        List<Long> orderIds = content.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = orderItemRepository
            .findByOrder_IdIn(orderIds)
            .stream()
            .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
        return orders.map(order ->
            buildOrderResponse(
                order,
                itemsByOrder.getOrDefault(order.getId(), List.of())
            )
        );
    }

    private OrderResponse buildOrderResponse(Order order) {
        return buildOrderResponse(
            order,
            orderItemRepository.findByOrder_Id(order.getId())
        );
    }

    private OrderResponse buildOrderResponse(Order order, List<OrderItem> items) {
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
        payment.setGatewayName(method != null ? method.name() : null);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCurrency("VND");

        if (method == PaymentMethod.MOMO_QR) {
            payment.setGatewayName("MOMO_QR");
            payment.setTransferNote(MomoTransferSupport.transferNote(order.getId()));
        }

        paymentRepository.save(payment);
    }

    private void clearCart(Cart cart) {

        cartItemRepository.hardDeleteAllByCartId(cart.getId());
    }
}

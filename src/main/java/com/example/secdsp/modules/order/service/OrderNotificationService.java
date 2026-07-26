package com.example.secdsp.modules.order.service;

import com.example.secdsp.modules.email.service.EmailService;
import com.example.secdsp.modules.order.entity.Order;
import com.example.secdsp.modules.order.entity.OrderItem;
import com.example.secdsp.modules.order.entity.OrderStatus;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gửi email vòng đời đơn cho người mua + từng người bán (không làm fail transaction).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationService {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public void notifyOrderCreated(Order order) {
        notifyParties(order, OrderStatus.PENDING, "Don moi — cho xac nhan",
            "Don hang da duoc tao va dang cho nguoi ban xac nhan.");
    }

    public void notifyStatusChanged(Order order, OrderStatus status) {
        notifyParties(order, status, statusLabel(status), statusMessage(status));
    }

    public void notifyCancelled(Order order, String reason) {
        notifyParties(order, OrderStatus.CANCELLED, "Don da huy",
            reason != null ? reason : "Don hang da bi huy.");
    }

    private void notifyParties(Order order, OrderStatus status, String statusLabel, String message) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        String itemsHtml = buildItemsHtml(items);
        String detail = "<p>" + message + "</p>"
            + "<p>Tong tien: <strong>" + money(order.getTotalAmount()) + " VND</strong></p>"
            + "<p>Dia chi: " + escape(order.getShippingAddress()) + "</p>"
            + itemsHtml;

        // Buyer
        User buyer = order.getUser();
        if (buyer != null && buyer.getId() != null) {
            userRepository.findById(buyer.getId()).ifPresent(u ->
                safeSend(u.getEmail(), displayName(u), "Nguoi mua", order.getId(), statusLabel, detail)
            );
        }

        // Sellers (unique)
        Set<Long> sellerIds = new HashSet<>();
        for (OrderItem item : items) {
            if (item.getSeller() != null && item.getSeller().getId() != null) {
                sellerIds.add(item.getSeller().getId());
            }
        }
        for (Long sellerId : sellerIds) {
            userRepository.findById(sellerId).ifPresent(u ->
                safeSend(u.getEmail(), displayName(u), "Nguoi ban", order.getId(), statusLabel, detail)
            );
        }
    }

    private void safeSend(
        String email,
        String name,
        String role,
        Long orderId,
        String statusLabel,
        String detail
    ) {
        if (email == null || email.isBlank()) {
            log.warn("Skip order email: missing address for order {}", orderId);
            return;
        }
        try {
            emailService.sendOrderLifecycleEmail(email, name, role, orderId, statusLabel, detail);
        } catch (Exception e) {
            log.error("Failed order email to {} for order {}: {}", email, orderId, e.getMessage());
        }
    }

    private static String statusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Cho xac nhan";
            case PAID -> "Da thanh toan";
            case PROCESSING -> "Da xac nhan / dang xu ly";
            case SHIPPING -> "Dang giao hang";
            case DELIVERED -> "Da giao thanh cong";
            case CANCELLED -> "Da huy";
            case REFUNDED -> "Hoan tien";
        };
    }

    private static String statusMessage(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Don hang dang cho xac nhan.";
            case PAID -> "Thanh toan thanh cong. Shop se xu ly don.";
            case PROCESSING -> "Nguoi ban da xac nhan va dang chuan bi hang.";
            case SHIPPING -> "Don hang dang tren duong giao den ban.";
            case DELIVERED -> "Don da giao thanh cong. Ban co the danh gia san pham trong 30 ngay.";
            case CANCELLED -> "Don hang da bi huy.";
            case REFUNDED -> "Don hang da duoc hoan tien.";
        };
    }

    private static String buildItemsHtml(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<ul style=\"padding-left:18px;margin:8px 0;\">");
        for (OrderItem i : items) {
            sb.append("<li>")
                .append(escape(i.getProductNameAtPurchase()))
                .append(" x")
                .append(i.getQuantity())
                .append(" — ")
                .append(money(i.getSubtotal()))
                .append(" VND</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private static String displayName(User u) {
        if (u.getFullName() != null && !u.getFullName().isBlank()) {
            return u.getFullName();
        }
        return u.getEmail();
    }

    private static String money(BigDecimal v) {
        if (v == null) return "0";
        return v.stripTrailingZeros().toPlainString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

package com.example.secdsp.modules.order.service;

import com.example.secdsp.modules.email.service.EmailService;
import com.example.secdsp.modules.notification.service.CustomerNotificationService;
import com.example.secdsp.modules.order.entity.Order;
import com.example.secdsp.modules.order.entity.OrderItem;
import com.example.secdsp.modules.order.entity.OrderStatus;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Gửi email vòng đời đơn cho người mua + từng người bán (không làm fail transaction).
 * Đọc DB trên thread request, gửi mail trên pool — không chặn cập nhật trạng thái.
 */
@Slf4j
@Component
public class OrderNotificationService {

    private static final class MailJob {
        private final String email;
        private final String name;
        private final String role;

        private MailJob(String email, String name, String role) {
            this.email = email;
            this.name = name;
            this.role = role;
        }

        private String email() {
            return email;
        }

        private String name() {
            return name;
        }

        private String role() {
            return role;
        }
    }

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerNotificationService customerNotificationService;
    private final Executor mailExecutor;

    public OrderNotificationService(
        EmailService emailService,
        UserRepository userRepository,
        OrderItemRepository orderItemRepository,
        CustomerNotificationService customerNotificationService,
        @Qualifier("mailExecutor") Executor mailExecutor
    ) {
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerNotificationService = customerNotificationService;
        this.mailExecutor = mailExecutor;
    }

    public void notifyOrderCreated(Order order) {
        notifyParties(order, OrderStatus.PENDING, "Đơn mới — chờ xác nhận",
            "Đơn hàng đã được tạo và đang chờ người bán xác nhận / thanh toán.");
    }

    public void notifyStatusChanged(Order order, OrderStatus status) {
        notifyParties(order, status, statusLabel(status), statusMessage(status));
    }

    public void notifyCancelled(Order order, String reason) {
        notifyParties(order, OrderStatus.CANCELLED, "Đơn đã hủy",
            reason != null ? reason : "Đơn hàng đã bị hủy.");
    }

    private void notifyParties(Order order, OrderStatus status, String statusLabel, String message) {
        Long orderId = order.getId();
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        String itemsHtml = buildItemsHtml(items);
        String detail = "<p>" + message + "</p>"
            + "<p>Tổng tiền: <strong>" + money(order.getTotalAmount()) + " VND</strong></p>"
            + "<p>Địa chỉ: " + escape(order.getShippingAddress()) + "</p>"
            + itemsHtml;

        List<MailJob> jobs = new ArrayList<>();

        User buyer = order.getUser();
        if (buyer != null && buyer.getId() != null) {
            userRepository.findById(buyer.getId()).ifPresent(u -> {
                jobs.add(new MailJob(u.getEmail(), displayName(u), "Người mua"));
                customerNotificationService.createOrderStatusNotification(
                    u.getId(),
                    orderId,
                    "Đơn #" + orderId + " · " + statusLabel,
                    message + " Hỏi chatbot \"đơn " + orderId + "\" để xem chi tiết."
                );
            });
        }

        Set<Long> sellerIds = new HashSet<>();
        for (OrderItem item : items) {
            if (item.getSeller() != null && item.getSeller().getId() != null) {
                sellerIds.add(item.getSeller().getId());
            }
        }
        for (Long sellerId : sellerIds) {
            userRepository.findById(sellerId).ifPresent(u -> {
                jobs.add(new MailJob(u.getEmail(), displayName(u), "Người bán"));
                String sellerTitle = status == OrderStatus.PENDING
                    ? "🛒 Khách vừa đặt đơn #" + orderId
                    : "Đơn #" + orderId + " · " + statusLabel;
                String sellerMsg = status == OrderStatus.PENDING
                    ? "Shop có đơn hàng mới từ khách " + (buyer != null && buyer.getFullName() != null ? buyer.getFullName() : "") + ". Tổng tiền: " + money(order.getTotalAmount()) + " VND. Hỏi chatbot \"đơn " + orderId + "\" để xem chi tiết."
                    : message + " Hỏi chatbot \"đơn " + orderId + "\" để xem chi tiết.";
                customerNotificationService.createOrderStatusNotification(
                    sellerId,
                    orderId,
                    sellerTitle,
                    sellerMsg
                );
            });
        }

        for (MailJob job : jobs) {
            mailExecutor.execute(() ->
                safeSend(job.email(), job.name(), job.role(), orderId, statusLabel, detail)
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
            log.info("Order email queued/sent to {} for order #{} [{}]", email, orderId, statusLabel);
        } catch (Exception e) {
            log.error("Failed order email to {} for order {}: {}", email, orderId, e.getMessage());
        }
    }

    private static String statusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case PAID -> "Đã thanh toán";
            case PROCESSING -> "Đã xác nhận / đang xử lý";
            case SHIPPING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao thành công";
            case CANCELLED -> "Đơn đã hủy";
            case REFUNDED -> "Hoàn tiền";
        };
    }

    private static String statusMessage(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Đơn hàng đang chờ xác nhận.";
            case PAID -> "Thanh toán thành công qua cổng thanh toán. Shop sẽ xử lý đơn.";
            case PROCESSING -> "Người bán đã xác nhận và đang chuẩn bị hàng.";
            case SHIPPING -> "Đơn hàng đang trên đường giao đến bạn.";
            case DELIVERED -> "Đơn đã giao thành công. Bạn có thể đánh giá sản phẩm trong 30 ngày.";
            case CANCELLED -> "Đơn hàng đã bị hủy.";
            case REFUNDED -> "Đơn hàng đã được hoàn tiền.";
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

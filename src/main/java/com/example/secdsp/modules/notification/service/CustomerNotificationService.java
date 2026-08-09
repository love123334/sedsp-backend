package com.example.secdsp.modules.notification.service;

import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.notification.dto.CustomerNotificationResponse;
import com.example.secdsp.modules.notification.entity.CustomerNotification;
import com.example.secdsp.modules.notification.repository.CustomerNotificationRepository;
import com.example.secdsp.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    private final CustomerNotificationRepository notificationRepository;

    @Transactional
    public void createOrderStatusNotification(
        Long userId,
        Long orderId,
        String title,
        String message
    ) {
        if (userId == null || title == null || title.isBlank()) {
            return;
        }
        CustomerNotification notification = new CustomerNotification();
        User user = new User();
        user.setId(userId);
        notification.setUser(user);
        notification.setOrderId(orderId);
        notification.setNotificationType("ORDER_STATUS");
        notification.setTitle(title.trim());
        notification.setMessage(message != null && !message.isBlank() ? message.trim() : title.trim());
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<CustomerNotificationResponse> listUnread(Long afterId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<CustomerNotification> rows = notificationRepository.findUnreadAfter(userId, afterId);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        return notificationRepository.countByUser_IdAndIsReadFalse(SecurityUtils.getCurrentUserId());
    }

    @Transactional
    public void markRead(Long notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CustomerNotification row = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!userId.equals(row.getUser().getId())) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
        row.setIsRead(true);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllReadForUser(SecurityUtils.getCurrentUserId());
    }

    private CustomerNotificationResponse toResponse(CustomerNotification n) {
        return CustomerNotificationResponse.builder()
            .id(n.getId())
            .orderId(n.getOrderId())
            .type(n.getNotificationType())
            .title(n.getTitle())
            .message(n.getMessage())
            .read(Boolean.TRUE.equals(n.getIsRead()))
            .createdAt(n.getCreatedAt())
            .build();
    }
}

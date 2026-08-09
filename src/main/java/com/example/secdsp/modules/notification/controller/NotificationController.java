package com.example.secdsp.modules.notification.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.notification.dto.CustomerNotificationResponse;
import com.example.secdsp.modules.notification.service.CustomerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final CustomerNotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<CustomerNotificationResponse>>> listUnread(
        @RequestParam(required = false) Long afterId
    ) {
        return ResponseEntity.ok(BaseResponse.success(notificationService.listUnread(afterId)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Map<String, Long>>> unreadCount() {
        return ResponseEntity.ok(
            BaseResponse.success(Map.of("count", notificationService.countUnread()))
        );
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}

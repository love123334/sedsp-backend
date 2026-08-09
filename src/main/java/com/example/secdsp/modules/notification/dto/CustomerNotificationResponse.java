package com.example.secdsp.modules.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class CustomerNotificationResponse {
    private Long id;
    private Long orderId;
    private String type;
    private String title;
    private String message;
    private boolean read;
    private OffsetDateTime createdAt;
}

package com.example.secdsp.modules.user.dto.internal;

import com.example.secdsp.modules.user.entity.UserStatus;

public record UserInfo(
    Long id,
    UserStatus status,
    String role
) {
}
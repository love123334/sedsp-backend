package com.example.secdsp.modules.order.dto.internal;

import lombok.Builder;

@Builder
public record OrderDashboardInfo(
    long pending,
    long processing,
    long shipping,
    long delivered
) {}
package com.example.secdsp.modules.sellerdashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RevenueSummary(

    BigDecimal totalRevenue,

    Long completedOrders

) {}
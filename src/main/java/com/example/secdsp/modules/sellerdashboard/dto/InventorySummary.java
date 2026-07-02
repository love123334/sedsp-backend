package com.example.secdsp.modules.sellerdashboard.dto;

import lombok.Builder;

@Builder
public record InventorySummary(

    long lowStockProducts,

    long outOfStockProducts

) {}
package com.example.secdsp.modules.inventory.dto.internal;

import lombok.Builder;

@Builder
public record InventorySummaryInfo(
    long lowStockProducts,
    long outOfStockProducts
) {}
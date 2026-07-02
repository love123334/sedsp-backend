package com.example.secdsp.modules.product.dto.internal;

import lombok.Builder;

@Builder
public record LowStockProductInfo(
    Long productId,
    String productName,
    Integer quantity
) {}
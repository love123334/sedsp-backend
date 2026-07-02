package com.example.secdsp.modules.product.dto.internal;

import lombok.Builder;

@Builder
public record ProductSummaryInfo(
    long totalProducts,
    long activeProducts
) {}
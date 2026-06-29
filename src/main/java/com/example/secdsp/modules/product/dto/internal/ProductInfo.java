package com.example.secdsp.modules.product.dto.internal;

import com.example.secdsp.modules.product.entity.ProductStatus;

public record ProductInfo(
    Long id,
    Long sellerId,
    ProductStatus status
) {}
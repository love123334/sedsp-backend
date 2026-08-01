package com.example.secdsp.modules.product.dto.internal;

import com.example.secdsp.modules.product.entity.ProductStatus;

import java.math.BigDecimal;

public record ProductInfo(
    Long id,
    Long sellerId,
    String name,
    BigDecimal price,
    BigDecimal costPrice,
    ProductStatus status
) {}
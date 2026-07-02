package com.example.secdsp.modules.order.dto.internal;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TopProductSalesInfo(

    Long productId,

    String productName,

    Long quantitySold,

    BigDecimal revenue

) {}

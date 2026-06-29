package com.example.secdsp.modules.sellerdashboard.dto;

import lombok.Builder;

@Builder
public record LowStockProductResponse(

    Long productId,

    String productName,

    Integer quantity

) {}
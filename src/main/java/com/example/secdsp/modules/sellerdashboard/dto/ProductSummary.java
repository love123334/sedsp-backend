package com.example.secdsp.modules.sellerdashboard.dto;

import lombok.Builder;

@Builder
public record ProductSummary(

    long totalProducts,

    long activeProducts

) {}
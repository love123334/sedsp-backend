package com.example.secdsp.modules.sellerdashboard.dto;

import lombok.Builder;

@Builder
public record OrderSummary(

    long pending,

    long processing,

    long shipping,

    long delivered

) {}
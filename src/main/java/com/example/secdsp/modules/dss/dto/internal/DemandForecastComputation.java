package com.example.secdsp.modules.dss.dto.internal;

import java.util.List;
import java.util.Map;

public record DemandForecastComputation(
    Long productId,
    String productName,
    int historicalDays,
    int forecastDays,
    double averageDailyDemand,
    long predictedDemand,
    String method,
    boolean insufficientData,
    List<Map<String, Object>> historicalSales,
    List<Map<String, Object>> forecastSales,
    Map<String, Object> featureSnapshot,
    String generatedAt
) {}

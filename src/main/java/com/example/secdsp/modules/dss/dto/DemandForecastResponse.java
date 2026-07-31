package com.example.secdsp.modules.dss.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DemandForecastResponse {

    Long productId;
    String productName;
    int historicalDays;
    int forecastDays;
    double averageDailyDemand;
    long predictedDemand;
    String method;
    boolean insufficientData;
    List<Map<String, Object>> historicalSales;
    List<Map<String, Object>> forecastSales;
    String generatedAt;
}

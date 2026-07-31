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
public class InventoryRecommendationResponse {

    int planningDays;
    String overallStatus;
    String recommendationMessage;
    List<Map<String, Object>> rows;
    String generatedAt;
}

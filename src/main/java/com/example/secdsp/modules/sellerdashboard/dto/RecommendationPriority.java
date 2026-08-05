package com.example.secdsp.modules.sellerdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = """
        Priority level of a recommendation.

        Available values:
        - HIGH : Immediate action is required.
        - MEDIUM : Action is recommended soon.
        - LOW : Minor recommendation.
        - INFO : Informational recommendation.
        """,
    implementation = RecommendationPriority.class
)
public enum RecommendationPriority {
    HIGH,
    MEDIUM,
    LOW,
    INFO
}
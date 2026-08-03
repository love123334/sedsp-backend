package com.example.secdsp.modules.sellerdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(
    description = "Actionable recommendation generated from seller dashboard analytics."
)
public record RecommendationResponse(

    @Schema(description = "Recommendation identifier.", example = "REC_LOW_STOCK")
    String id,

    @Schema(description = "Recommendation title.", example = "Low inventory warning")
    String title,

    @Schema(
        description = "Recommendation message.",
        example = "Product 'Wireless Mouse' is running low on stock. Restock soon to avoid losing sales."
    )
    String message,

    @Schema(
        description = """
            Available values:
            - HIGH : Immediate action is required.
            - MEDIUM : Action is recommended soon.
            - LOW : Minor recommendation.
            - INFO : Informational recommendation.
            """,
        implementation = RecommendationPriority.class
    )
    RecommendationPriority priority,

    @Schema(
        description = "URL for the recommended action.",
        example = "/seller/inventory?filter=LOW_STOCK"
    )
    String actionUrl,

    @Schema(
        description = "Action button label.",
        example = "Restock inventory"
    )
    String actionLabel
) {
}
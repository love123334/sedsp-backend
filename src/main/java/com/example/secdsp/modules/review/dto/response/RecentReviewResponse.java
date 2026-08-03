package com.example.secdsp.modules.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(
    description = "Recent customer review."
)
public record RecentReviewResponse(
    @Schema(description = "Review ID.", example = "501")
    Long reviewId,

    @Schema(description = "Product ID.", example = "21")
    Long productId,

    @Schema(description = "Product name.", example = "Wireless Mouse")
    String productName,

    @Schema(description = "Rating score.", example = "5")
    Integer rating,

    @Schema(
        description = "Customer review comment.",
        example = "Excellent quality and fast delivery."
    )
    String comment,

    @Schema(
        description = "Review creation timestamp.",
        example = "2026-08-03T10:15:30Z"
    )
    OffsetDateTime createdAt
) {
}

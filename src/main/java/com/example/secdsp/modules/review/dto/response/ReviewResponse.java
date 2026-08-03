package com.example.secdsp.modules.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Product review information.")
public record ReviewResponse(

    @Schema(
        description = "Review ID.",
        example = "10"
    )
    Long id,

    @Schema(
        description = "Reviewer user ID.",
        example = "25"
    )
    Long userId,

    @Schema(
        description = "Reviewer full name.",
        example = "John Smith"
    )
    String userName,

    @Schema(
        description = "Rating score.",
        example = "5"
    )
    Integer rating,

    @Schema(
        description = "Review comment.",
        example = "Excellent product. Worth buying."
    )
    String comment,

    @Schema(
        description = "Review creation timestamp.",
        example = "2026-08-03T14:30:00Z"
    )
    OffsetDateTime createdAt
) {
}
package com.example.secdsp.modules.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product rating summary.")
public record RatingSummaryResponse(

    @Schema(
        description = "Average rating.",
        example = "4.8"
    )
    Double averageRating,

    @Schema(
        description = "Total number of reviews.",
        example = "156"
    )
    Long totalReviews
) {
}
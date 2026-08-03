package com.example.secdsp.modules.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Rating distribution item."
)
public record RatingBreakdownItem(
    @Schema(description = "Rating score.", example = "5")
    Integer rating,

    @Schema(description = "Number of reviews.", example = "180")
    Long count,

    @Schema(description = "Percentage of total reviews.", example = "62.5")
    Double percentage
) {
}

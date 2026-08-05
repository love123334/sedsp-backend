package com.example.secdsp.modules.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Request for updating a product review.")
public record UpdateReviewRequest(

    @Schema(
        description = "Updated rating from 1 to 5.",
        example = "4",
        minimum = "1",
        maximum = "5"
    )
    @Min(1)
    @Max(5)
    Integer rating,

    @Schema(
        description = "Updated review comment.",
        example = "Still a good product after using it for several weeks."
    )
    String comment
) {
}
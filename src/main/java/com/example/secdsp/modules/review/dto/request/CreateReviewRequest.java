package com.example.secdsp.modules.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Request for creating a product review.")
public record CreateReviewRequest(

    @Schema(
        description = "Product rating from 1 to 5.",
        example = "5",
        minimum = "1",
        maximum = "5"
    )
    @Min(1)
    @Max(5)
    Integer rating,

    @Schema(
        description = "Review comment.",
        example = "Excellent product. Highly recommended."
    )
    String comment
) {
}
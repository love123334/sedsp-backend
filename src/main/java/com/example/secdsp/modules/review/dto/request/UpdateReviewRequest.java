package com.example.secdsp.modules.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateReviewRequest(
    @Min(1) @Max(5)
    Integer rating,
    String comment
) {}
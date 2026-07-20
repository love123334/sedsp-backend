package com.example.secdsp.modules.review.dto.response;

public record RatingSummaryResponse(
    Double averageRating,
    Long totalReviews
) {}

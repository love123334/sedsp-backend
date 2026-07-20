package com.example.secdsp.modules.review.dto.response;

public record RatingBreakdownItem(
    Integer rating,
    Long count,
    Double percentage
) {}

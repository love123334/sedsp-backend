package com.example.secdsp.modules.review.dto.response;

import java.time.OffsetDateTime;

public record ReviewResponse(
    Long id,
    Long userId,
    String userName,
    Integer rating,
    String comment,
    OffsetDateTime createdAt
) {
}

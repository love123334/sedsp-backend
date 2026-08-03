package com.example.secdsp.modules.review.dto.response;

import java.time.OffsetDateTime;

public record RecentReviewResponse(
    Long reviewId,
    Long productId,
    String productName,
    Integer rating,
    String comment,
    OffsetDateTime createdAt
) {
}

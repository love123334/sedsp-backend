package com.example.secdsp.modules.review.dto.response;

import java.time.LocalDateTime;

public record RecentReviewResponse(
    Long reviewId,
    Long productId,
    String productName,
    Integer rating,
    String comment,
    LocalDateTime createdAt
) {}

package com.example.secdsp.modules.sellerdashboard.dto;

import com.example.secdsp.modules.review.dto.response.RatingBreakdownItem;
import com.example.secdsp.modules.review.dto.response.RecentReviewResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record SellerRatingSummary(
    Double averageRating,
    Long totalReviews,
    List<RatingBreakdownItem> ratingBreakdown,
    List<RecentReviewResponse> recentReviews,
    String warning
) {}
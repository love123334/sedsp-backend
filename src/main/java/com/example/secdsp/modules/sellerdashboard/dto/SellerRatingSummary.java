package com.example.secdsp.modules.sellerdashboard.dto;

import com.example.secdsp.modules.review.dto.response.RatingBreakdownItem;
import com.example.secdsp.modules.review.dto.response.RecentReviewResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(
    description = "Seller rating statistics and recent customer reviews."
)
public record SellerRatingSummary(
    @Schema(description = "Average seller rating.", example = "4.7")
    Double averageRating,

    @Schema(description = "Total number of reviews.", example = "356")
    Long totalReviews,

    @Schema(description = "Rating distribution.")
    List<RatingBreakdownItem> ratingBreakdown,

    @Schema(description = "Most recent customer reviews.")
    List<RecentReviewResponse> recentReviews,

    @Schema(
        description = "Rating warning message when seller performance requires attention.",
        example = "Average rating is below 3.5 stars. Please improve customer service."
    )
    String warning
) {
}
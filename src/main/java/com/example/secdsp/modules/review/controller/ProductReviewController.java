package com.example.secdsp.modules.review.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.review.dto.request.CreateReviewRequest;
import com.example.secdsp.modules.review.dto.request.UpdateReviewRequest;
import com.example.secdsp.modules.review.dto.response.RatingSummaryResponse;
import com.example.secdsp.modules.review.dto.response.ReviewResponse;
import com.example.secdsp.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
        @PathVariable Long productId,
        @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                reviewService.createReview(productId, request)
            )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
        @PathVariable Long productId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                reviewService.getReviews(productId, pageable)
            )
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getSummary(
        @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                reviewService.getRatingSummary(productId)
            )
        );
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
        @PathVariable Long productId,
        @PathVariable Long reviewId,
        @Valid @RequestBody UpdateReviewRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                reviewService.updateReview(reviewId, request)
            )
        );
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
        @PathVariable Long productId,
        @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }
}

package com.example.secdsp.modules.review.service;

import com.example.secdsp.modules.review.dto.request.CreateReviewRequest;
import com.example.secdsp.modules.review.dto.request.UpdateReviewRequest;
import com.example.secdsp.modules.review.dto.response.RatingSummaryResponse;
import com.example.secdsp.modules.review.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(Long productId, CreateReviewRequest request);

    ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request);

    void deleteReview(Long reviewId);

    Page<ReviewResponse> getReviews(Long productId, Pageable pageable);

    RatingSummaryResponse getRatingSummary(Long productId);
}

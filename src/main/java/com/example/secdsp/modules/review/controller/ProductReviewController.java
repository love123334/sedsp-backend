package com.example.secdsp.modules.review.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.review.dto.request.CreateReviewRequest;
import com.example.secdsp.modules.review.dto.request.UpdateReviewRequest;
import com.example.secdsp.modules.review.dto.response.RatingSummaryResponse;
import com.example.secdsp.modules.review.dto.response.ReviewResponse;
import com.example.secdsp.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "APIs for managing product reviews.")
public class ProductReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Create product review",
        description = "Create a new review for a product."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid review data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Review already exists", content = @Content)
    })
    public ResponseEntity<BaseResponse<ReviewResponse>> createReview(
        @Parameter(description = "Product ID", example = "1")
        @PathVariable Long productId,

        @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                reviewService.createReview(productId, request)
            )
        );
    }

    @GetMapping
    @Operation(
        summary = "Get product reviews",
        description = "Retrieve paginated reviews for a product."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<BaseResponse<Page<ReviewResponse>>> getReviews(
        @Parameter(description = "Product ID", example = "1")
        @PathVariable Long productId,

        @ParameterObject
        @Parameter(description = "Pagination information")
        Pageable pageable
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                reviewService.getReviews(productId, pageable)
            )
        );
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Get rating summary",
        description = "Retrieve the rating summary for a product."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rating summary retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<BaseResponse<RatingSummaryResponse>> getSummary(
        @Parameter(description = "Product ID", example = "1")
        @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                reviewService.getRatingSummary(productId)
            )
        );
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Update review",
        description = "Update an existing product review."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid review data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "Review not found", content = @Content)
    })
    public ResponseEntity<BaseResponse<ReviewResponse>> updateReview(
        @Parameter(description = "Product ID", example = "1")
        @PathVariable Long productId,

        @Parameter(description = "Review ID", example = "10")
        @PathVariable Long reviewId,

        @Valid @RequestBody UpdateReviewRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                reviewService.updateReview(reviewId, request)
            )
        );
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Delete review",
        description = "Delete an existing product review."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "Review not found", content = @Content)
    })
    public ResponseEntity<BaseResponse<Void>> deleteReview(
        @Parameter(description = "Product ID", example = "1")
        @PathVariable Long productId,

        @Parameter(description = "Review ID", example = "10")
        @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(BaseResponse.success("Review deleted"));
    }
}
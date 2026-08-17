package com.example.secdsp.modules.review.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.order.entity.OrderStatus;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.review.dto.request.CreateReviewRequest;
import com.example.secdsp.modules.review.dto.request.UpdateReviewRequest;
import com.example.secdsp.modules.review.dto.response.RatingSummaryResponse;
import com.example.secdsp.modules.review.dto.response.ReviewResponse;
import com.example.secdsp.modules.review.entity.ProductReview;
import com.example.secdsp.modules.review.repository.ProductReviewRepository;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Override
    public ReviewResponse createReview(Long productId, CreateReviewRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();

        if (reviewRepository.existsByProduct_IdAndUser_Id(productId, userId)) {
            throw new BusinessException("You already reviewed this product");
        }

        boolean purchased = orderItemRepository
            .existsByOrder_User_IdAndProduct_IdAndOrder_Status(
                userId,
                productId,
                OrderStatus.DELIVERED
            );

        if (!purchased) {
            throw new BusinessException("You can only review products you purchased");
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        User user = userRepository.getReferenceById(userId);

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(request.comment());

        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request) {

        ProductReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (!review.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You cannot update this review");
        }

        review.setRating(request.rating());
        review.setComment(request.comment());

        return mapToResponse(review);
    }

    @Override
    public void deleteReview(Long reviewId) {

        ProductReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (!review.getUser().getId().equals(currentUserId)
            && !SecurityUtils.hasRole(UserRole.ADMIN)) {
            throw new UnauthorizedException("You cannot delete this review");
        }

        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(Long productId, Pageable pageable) {

        return reviewRepository.findByProduct_Id(productId, pageable)
            .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(Long productId) {

        List<Object[]> rows = reviewRepository.getRatingSummary(productId);
        Object[] result = rows == null || rows.isEmpty()
            ? new Object[] { null, 0L }
            : rows.get(0);

        double avg = result.length > 0 && result[0] instanceof Number value
            ? value.doubleValue()
            : 0.0;
        long count = result.length > 1 && result[1] instanceof Number value
            ? value.longValue()
            : 0L;

        return new RatingSummaryResponse(avg, count);
    }

    private ReviewResponse mapToResponse(ProductReview review) {
        return new ReviewResponse(
            review.getId(),
            review.getUser().getId(),
            review.getUser().getFullName(),
            review.getRating(),
            review.getComment(),
            review.getCreatedAt()
        );
    }
}

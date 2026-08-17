package com.example.secdsp.modules.review.repository;

import com.example.secdsp.modules.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    boolean existsByProduct_IdAndUser_Id(Long productId, Long userId);

    Optional<ProductReview> findByProduct_IdAndUser_Id(Long productId, Long userId);

    Page<ProductReview> findByProduct_Id(Long productId, Pageable pageable);

    @Query("""
            SELECT AVG(r.rating), COUNT(r)
            FROM ProductReview r
            WHERE r.product.id = :productId
        """)
    List<Object[]> getRatingSummary(@Param("productId") Long productId);

    @Query("""
            select avg(r.rating), count(r)
            from ProductReview r
            where r.product.seller.id = :sellerId
        """)
    List<Object[]> getSellerRatingSummary(Long sellerId);

    @Query("""
            select r.rating, count(r)
            from ProductReview r
            where r.product.seller.id = :sellerId
            group by r.rating
        """)
    List<Object[]> getSellerRatingBreakdown(Long sellerId);

    List<ProductReview>
    findTop5ByProduct_Seller_IdOrderByCreatedAtDesc(Long sellerId);
}

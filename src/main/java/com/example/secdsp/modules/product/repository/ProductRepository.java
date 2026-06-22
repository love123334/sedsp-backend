package com.example.secdsp.modules.product.repository;

import com.example.secdsp.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("""
        SELECT p FROM Product p
        WHERE p.deletedAt IS NULL
        AND p.id = :id
    """)
    Optional<Product> findByIdAndDeletedAtIsNull(@Param("id") Long id);


    boolean existsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);


    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("""
        SELECT p FROM Product p
        WHERE p.deletedAt IS NULL
        AND (:keyword IS NULL OR :keyword = ''
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:sellerId IS NULL OR p.seller.id = :sellerId)
    """)
    Page<Product> searchProducts(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("sellerId") Long sellerId,
        Pageable pageable
    );


    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("""
        SELECT p FROM Product p
        WHERE p.seller.id = :sellerId
        AND p.deletedAt IS NULL
    """)
    Page<Product> findBySellerIdAndDeletedAtIsNull(
        @Param("sellerId") Long sellerId,
        Pageable pageable
    );


    Optional<Product> findByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
        String name,
        Long id
    );


    Optional<Product> findBySlugIgnoreCaseAndIdNotAndDeletedAtIsNull(
        String slug,
        Long id
    );


    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
package com.example.secdsp.modules.product.repository;

import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "seller", "productImages"})
    Optional<Product> findById(Long id);

    boolean existsBySlugIgnoreCase(String slug);

    @EntityGraph(attributePaths = {"category", "seller", "productImages"})
    @Query("""
            SELECT DISTINCT p
            FROM Product p
            WHERE (:keyword IS NULL OR :keyword = ''
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

    @EntityGraph(attributePaths = {"category", "seller", "productImages"})
    List<Product> findByIdIn(List<Long> ids);

    @Query("""
        select distinct p from Product p
        left join fetch p.seller
        where p.id in :ids
        """)
    List<Product> findAllWithSellerByIdIn(@Param("ids") Collection<Long> ids);


    @EntityGraph(attributePaths = {"category", "seller", "productImages"})
    Page<Product> findBySeller_Id(
        Long sellerId,
        Pageable pageable
    );


    Optional<Product> findByNameIgnoreCaseAndIdNot(
        String name,
        Long id
    );


    Optional<Product> findBySlugIgnoreCaseAndIdNot(
        String slug,
        Long id
    );

    boolean existsByCategory_Id(Long categoryId);

    long countBySeller_Id(Long sellerId);

    long countBySeller_IdAndStatus(Long sellerId, ProductStatus status);

    @Query("""
        select p
        from Product p
        join Inventory i on i.product.id = p.id
        where p.seller.id = :sellerId
        and i.availableQuantity <= 5
        """)
    List<Product> findLowStockProductsBySeller(Long sellerId);
}
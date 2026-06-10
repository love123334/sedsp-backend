package com.example.secdsp.modules.category.repository;

import com.example.secdsp.modules.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL AND c.id = :id")
    Optional<Category> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL AND c.parent IS NULL ORDER BY c.name ASC")
    List<Category> findRootCategories();

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.deletedAt IS NULL " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Category> searchCategories(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByNameAndDeletedAtIsNull(String name);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.deletedAt IS NULL AND c.id <> :id")
    Optional<Category> findByNameAndIdNotAndDeletedAtIsNull(@Param("name") String name, @Param("id") Long id);

    @Query("SELECT c FROM Category c WHERE c.slug = :slug AND c.deletedAt IS NULL AND c.id <> :id")
    Optional<Category> findBySlugAndIdNotAndDeletedAtIsNull(@Param("slug") String slug, @Param("id") Long id);

    @Query("SELECT COUNT(p) FROM com.example.secdsp.modules.product.entity.Product p WHERE p.category.id = :categoryId AND p.deletedAt IS NULL")
    long countActiveProductsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL")
    List<Category> findAllActiveCategories();
}

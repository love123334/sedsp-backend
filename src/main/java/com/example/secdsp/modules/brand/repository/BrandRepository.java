package com.example.secdsp.modules.brand.repository;

import com.example.secdsp.modules.brand.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    @Query("SELECT b FROM Brand b WHERE b.deletedAt IS NULL AND b.id = :id")
    Optional<Brand> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    @Query("""
        SELECT b FROM Brand b 
        WHERE b.deletedAt IS NULL
        AND (
            :keyword IS NULL OR :keyword = '' 
            OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
            OR LOWER(b.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """)
    Page<Brand> searchBrands(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, Long id);

    boolean existsBySlugIgnoreCaseAndIdNotAndDeletedAtIsNull(String slug, Long id);
}

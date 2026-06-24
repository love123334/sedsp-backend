package com.example.secdsp.modules.category.repository;

import com.example.secdsp.modules.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNullOrderByNameAsc();

    @EntityGraph(attributePaths = "parent")
    @Query("""
            SELECT c FROM Category c
            WHERE (:keyword IS NULL OR :keyword = '' 
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
                   OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Category> searchCategories(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByNameAndParent_Id(String name, Long parentId);

    boolean existsByNameAndParentIsNull(String name);

    boolean existsBySlug(String slug);

    Optional<Category> findByNameAndIdNot(String name, Long id);

    Optional<Category> findBySlugAndIdNot(String slug, Long id);

    @Query("""
            SELECT DISTINCT c
            FROM Category c
            LEFT JOIN FETCH c.parent
        """)
    List<Category> findAllWithParent();

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByNameAndParentIsNullAndIdNot(String name, Long id);

    Optional<Category> findByNameAndParent_IdAndIdNot(String name, Long parentId, Long id);

    boolean existsByParent_Id(Long parentId);
}
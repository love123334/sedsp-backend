package com.example.secdsp.modules.category.repository;

import com.example.secdsp.modules.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNullOrderByNameAsc();

    /**
     * Avoid EntityGraph + Page on self-referencing Category: Hibernate can throw
     * DataAccessException under SQLRestriction (prod open-in-view=false).
     */
    @Query("""
            SELECT c FROM Category c
            LEFT JOIN FETCH c.parent
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY c.name ASC
        """)
    List<Category> searchCategories(@Param("keyword") String keyword);

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
package com.example.secdsp.modules.product.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class ProductCatalogQueryRepositoryImpl implements ProductCatalogQueryRepository {

    private static final String SALES_JOIN = """
        LEFT JOIN (
            SELECT oi.product_id AS pid, COALESCE(SUM(oi.quantity), 0) AS sold_qty
            FROM order_items oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.status IN ('PAID', 'PROCESSING', 'SHIPPING', 'DELIVERED')
            GROUP BY oi.product_id
        ) sales ON sales.pid = p.id
        """;

    private static final String REVIEWS_JOIN = """
        LEFT JOIN (
            SELECT pr.product_id AS pid,
                   COALESCE(AVG(pr.rating), 0) AS avg_rating,
                   COUNT(*) AS review_cnt
            FROM product_reviews pr
            WHERE pr.deleted_at IS NULL
            GROUP BY pr.product_id
        ) reviews ON reviews.pid = p.id
        """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Long> searchProductIds(
        String keyword,
        Long categoryId,
        Long sellerId,
        String sort,
        Pageable pageable
    ) {
        FilterClause filter = buildFilterClause(keyword, categoryId, sellerId);
        String orderBy = resolveOrderBy(sort);

        String countSql = """
            SELECT COUNT(DISTINCT p.id)
            FROM products p
            """ + filter.sql();

        Query countQuery = entityManager.createNativeQuery(countSql);
        bindParams(countQuery, filter.params());
        Number total = (Number) countQuery.getSingleResult();

        String idSql = """
            SELECT p.id
            FROM products p
            """ + SALES_JOIN + REVIEWS_JOIN + filter.sql() + """
            ORDER BY """ + orderBy;

        Query idQuery = entityManager.createNativeQuery(idSql);
        bindParams(idQuery, filter.params());
        idQuery.setFirstResult((int) pageable.getOffset());
        idQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) idQuery.getResultList())
            .stream()
            .map(Number::longValue)
            .toList();

        return new PageImpl<>(ids, pageable, total.longValue());
    }

    private static FilterClause buildFilterClause(String keyword, Long categoryId, Long sellerId) {
        StringBuilder sql = new StringBuilder(" WHERE p.deleted_at IS NULL ");
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.hasText(keyword)) {
            sql.append("""
                 AND (
                    LOWER(p.name) LIKE LOWER(:keyword)
                    OR LOWER(p.slug) LIKE LOWER(:keyword)
                    OR LOWER(COALESCE(p.description, '')) LIKE LOWER(:keyword)
                 )
                """);
            params.put("keyword", "%" + keyword.trim() + "%");
        }
        if (categoryId != null) {
            sql.append(" AND p.category_id = :categoryId ");
            params.put("categoryId", categoryId);
        }
        if (sellerId != null) {
            sql.append(" AND p.seller_id = :sellerId ");
            params.put("sellerId", sellerId);
        }
        return new FilterClause(sql.toString(), params);
    }

    private static String resolveOrderBy(String sort) {
        String key = sort == null ? "popular" : sort.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "price-asc" -> " p.price ASC, p.id DESC ";
            case "price-desc" -> " p.price DESC, p.id DESC ";
            case "newest" -> " p.created_at DESC, p.id DESC ";
            case "rating-asc" -> " COALESCE(reviews.avg_rating, 0) ASC, p.id DESC ";
            case "rating-desc" -> " COALESCE(reviews.avg_rating, 0) DESC, p.id DESC ";
            default -> " COALESCE(sales.sold_qty, 0) DESC, p.id DESC ";
        };
    }

    private static void bindParams(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private record FilterClause(String sql, Map<String, Object> params) {}
}

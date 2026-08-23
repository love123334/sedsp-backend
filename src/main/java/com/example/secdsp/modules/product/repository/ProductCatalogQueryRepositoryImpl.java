package com.example.secdsp.modules.product.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Arrays;
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

    private static final java.util.Set<String> SEARCH_STOP_WORDS = java.util.Set.of(
        "cho", "minh", "mình", "toi", "tôi", "mua", "can", "cần", "tim", "tìm",
        "loai", "loại", "gia", "giá", "re", "rẻ", "dep", "đẹp", "xin", "xịn",
        "tot", "tốt", "nao", "nào", "co", "có", "khong", "không", "ko", "k",
        "voi", "với", "va", "và", "cua", "của", "ban", "bán", "xem", "tu", "tư",
        "van", "vấn", "hoi", "hỏi", "duoi", "dưới", "tren", "trên", "khoang", "khoảng",
        "tam", "tầm", "ngon", "nhat", "nhất", "chinh", "chính", "hang", "hãng",
        "cao", "cap", "cấp", "moi", "mới", "nhe", "nhé", "a", "ạ", "shop", "store", "em"
    );

    private static FilterClause buildFilterClause(String keyword, Long categoryId, Long sellerId) {
        StringBuilder sql = new StringBuilder("""
             WHERE p.deleted_at IS NULL
               AND p.status = 'ACTIVE'
            """);
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.hasText(keyword)) {
            String rawKw = keyword.trim();
            String lowerKw = rawKw.toLowerCase(Locale.ROOT);
            params.put("exactKeyword", "%" + lowerKw + "%");

            String[] rawTokens = lowerKw.split("[\\s,._\\-+:/]+");
            List<String> meaningfulTokens = Arrays.stream(rawTokens)
                .map(String::trim)
                .filter(t -> t.length() >= 2 && !SEARCH_STOP_WORDS.contains(t))
                .toList();

            sql.append("""
                 AND (
                    LOWER(p.name) LIKE :exactKeyword
                    OR LOWER(p.slug) LIKE :exactKeyword
                    OR LOWER(COALESCE(p.description, '')) LIKE :exactKeyword
                """);

            if (!meaningfulTokens.isEmpty()) {
                sql.append(" OR (");
                for (int i = 0; i < meaningfulTokens.size(); i++) {
                    String paramName = "tok_" + i;
                    if (i > 0) {
                        sql.append(" AND ");
                    }
                    sql.append(" (LOWER(p.name) LIKE :").append(paramName)
                       .append(" OR LOWER(p.slug) LIKE :").append(paramName)
                       .append(" OR LOWER(COALESCE(p.description, '')) LIKE :").append(paramName).append(") ");
                    params.put(paramName, "%" + meaningfulTokens.get(i) + "%");
                }
                sql.append(") ");
            }

            // Cross-category and synonym expansions
            if (lowerKw.contains("tai nghe") || lowerKw.contains("headphone") || lowerKw.contains("earbuds") || lowerKw.contains("chống ồn") || lowerKw.contains("chong on")) {
                sql.append(" OR (LOWER(p.slug) LIKE '%tai-nghe%' OR LOWER(p.slug) LIKE '%headphone%' OR LOWER(p.slug) LIKE '%airpods%') ");
            }
            if (lowerKw.contains("bàn phím") || lowerKw.contains("ban phim") || lowerKw.contains("keyboard") || lowerKw.contains("keypro")) {
                sql.append(" OR (LOWER(p.slug) LIKE '%ban-phim%' OR LOWER(p.slug) LIKE '%keyboard%' OR LOWER(p.slug) LIKE '%keypro%') ");
            }
            if (lowerKw.contains("nồi chiên") || lowerKw.contains("noi chien") || lowerKw.contains("air fryer") || lowerKw.contains("chiên không dầu")) {
                sql.append(" OR (LOWER(p.slug) LIKE '%noi-chien%' OR LOWER(p.slug) LIKE '%air-fryer%') ");
            }
            if (lowerKw.contains("giày") || lowerKw.contains("giay") || lowerKw.contains("chạy bộ") || lowerKw.contains("chay bo") || lowerKw.contains("sneaker") || lowerKw.contains("marathon")) {
                sql.append(" OR (LOWER(p.slug) LIKE '%giay%' OR LOWER(p.slug) LIKE '%shoes%' OR LOWER(p.slug) LIKE '%sneakers%' OR LOWER(p.slug) LIKE '%marathon%') ");
            }

            sql.append(" ) ");
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

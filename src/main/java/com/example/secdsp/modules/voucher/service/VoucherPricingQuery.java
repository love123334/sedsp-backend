package com.example.secdsp.modules.voucher.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Native SQL pricing lookups for voucher validation — avoids Hibernate
 * Tuple/Object[] and lazy-load issues on Railway PostgreSQL.
 */
@Repository
class VoucherPricingQuery {

    @PersistenceContext
    private EntityManager entityManager;

    record LineItem(Long sellerId, BigDecimal subtotal) {}

    @Transactional(readOnly = true)
    List<Long> expandProductIdsFromCart(Long cartId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
            .createNativeQuery(
                """
                SELECT ci.product_id, ci.quantity
                FROM cart_items ci
                WHERE ci.cart_id = :cartId
                """
            )
            .setParameter("cartId", cartId)
            .getResultList();

        List<Long> ids = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = asLong(row[0]);
            if (productId == null) {
                continue;
            }
            int qty = asInt(row[1]);
            for (int i = 0; i < qty; i++) {
                ids.add(productId);
            }
        }
        return ids;
    }

    Map<Long, LineItem> pricingLines(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new HashMap<>();
        for (Long productId : productIds) {
            if (productId == null) {
                continue;
            }
            counts.merge(productId, 1L, Long::sum);
        }
        if (counts.isEmpty()) {
            return Map.of();
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
            .createNativeQuery(
                """
                SELECT p.id, p.seller_id, p.price
                FROM products p
                WHERE p.id IN (:ids)
                """
            )
            .setParameter("ids", counts.keySet())
            .getResultList();

        Map<Long, LineItem> map = new HashMap<>();
        for (Object[] row : rows) {
            Long id = asLong(row[0]);
            Long sellerId = asLong(row[1]);
            BigDecimal price = asBigDecimal(row[2]);
            if (id == null || price == null) {
                continue;
            }
            long qty = counts.getOrDefault(id, 0L);
            map.put(id, new LineItem(sellerId, price.multiply(BigDecimal.valueOf(qty))));
        }
        return map;
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        throw new IllegalArgumentException("Expected numeric id, got " + value.getClass().getName());
    }

    private static int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text.trim());
        }
        throw new IllegalArgumentException("Expected numeric qty, got " + value.getClass().getName());
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text.trim());
        }
        throw new IllegalArgumentException("Expected numeric price, got " + value.getClass().getName());
    }
}

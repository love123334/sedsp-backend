package com.example.secdsp.modules.voucher.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** JDBC pricing lookups for voucher validation — predictable types on Railway. */
@Repository
@RequiredArgsConstructor
class VoucherPricingQuery {

    private final JdbcTemplate jdbcTemplate;

    record LineItem(Long sellerId, BigDecimal subtotal) {}

    @Transactional(readOnly = true)
    List<Long> expandProductIdsFromCart(Long cartId) {
        return jdbcTemplate.query(
            """
            SELECT ci.product_id, ci.quantity
            FROM cart_items ci
            WHERE ci.cart_id = ?
              AND ci.deleted_at IS NULL
            """,
            (rs, rowNum) -> {
                long productId = rs.getLong("product_id");
                int qty = rs.getInt("quantity");
                List<Long> ids = new ArrayList<>(Math.max(qty, 0));
                for (int i = 0; i < qty; i++) {
                    ids.add(productId);
                }
                return ids;
            },
            cartId
        ).stream().flatMap(List::stream).toList();
    }

    @Transactional(readOnly = true)
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

        List<Long> ids = new ArrayList<>(counts.keySet());
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        List<PricingRow> rows = jdbcTemplate.query(
            """
            SELECT p.id, p.seller_id, p.price
            FROM products p
            WHERE p.deleted_at IS NULL
              AND p.id IN (%s)
            """.formatted(placeholders),
            (rs, rowNum) -> readPricingRow(rs),
            ids.toArray()
        );

        Map<Long, LineItem> map = new HashMap<>();
        for (PricingRow row : rows) {
            long qty = counts.getOrDefault(row.id(), 0L);
            map.put(
                row.id(),
                new LineItem(row.sellerId(), row.price().multiply(BigDecimal.valueOf(qty)))
            );
        }
        return map;
    }

    private static PricingRow readPricingRow(ResultSet rs) throws SQLException {
        return new PricingRow(
            rs.getLong("id"),
            rs.getObject("seller_id") != null ? rs.getLong("seller_id") : null,
            rs.getBigDecimal("price")
        );
    }

    private record PricingRow(Long id, Long sellerId, BigDecimal price) {}
}

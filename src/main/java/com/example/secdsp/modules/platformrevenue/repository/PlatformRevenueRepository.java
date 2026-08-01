package com.example.secdsp.modules.platformrevenue.repository;

import com.example.secdsp.modules.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PlatformRevenueRepository extends JpaRepository<Order, Long> {

    @Query(value = """
        SELECT COALESCE(SUM(o.subtotal_amount)
                            FILTER (WHERE o.status = 'DELIVERED'), 0) AS gmv,
               COALESCE(SUM(o.total_amount)
                            FILTER (WHERE o.status = 'DELIVERED'), 0) AS delivered_order_value,
               COALESCE(SUM(o.discount_amount)
                            FILTER (WHERE o.status = 'DELIVERED'), 0) AS total_discount_amount,
               COALESCE(SUM(o.shipping_fee)
                            FILTER (WHERE o.status = 'DELIVERED'), 0) AS total_shipping_fee,
               COUNT(*) AS total_orders,
               COUNT(*) FILTER (WHERE o.status = 'DELIVERED') AS delivered_orders,
               COUNT(DISTINCT o.user_id)
                   FILTER (WHERE o.status = 'DELIVERED') AS active_customers
        FROM orders o
        WHERE o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        """, nativeQuery = true)
    Object[] findOrderOverview(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT COALESCE(SUM(o.subtotal_amount), 0)
        FROM orders o
        WHERE o.status = 'DELIVERED'
          AND o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        """, nativeQuery = true)
    BigDecimal findGrossMerchandiseValue(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT COALESCE(SUM(oi.quantity), 0) AS units_sold,
               COUNT(DISTINCT oi.seller_id) AS active_sellers
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        WHERE o.status = 'DELIVERED'
          AND o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        """, nativeQuery = true)
    Object[] findItemOverview(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT CAST(o.status AS TEXT) AS status,
               COUNT(*) AS order_count
        FROM orders o
        WHERE o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        GROUP BY o.status
        ORDER BY o.status
        """, nativeQuery = true)
    List<Object[]> findOrderStatusDistribution(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT CAST(o.created_at AS DATE) AS period_start,
               COALESCE(SUM(o.subtotal_amount), 0) AS gmv,
               COALESCE(SUM(o.total_amount), 0) AS delivered_order_value,
               COUNT(*) AS delivered_orders,
               COALESCE(SUM(item_totals.units_sold), 0) AS units_sold
        FROM orders o
        LEFT JOIN (
            SELECT oi.order_id,
                   SUM(oi.quantity) AS units_sold
            FROM order_items oi
            JOIN orders filtered_order ON filtered_order.id = oi.order_id
            WHERE filtered_order.status = 'DELIVERED'
              AND filtered_order.created_at >= :startDateTime
              AND filtered_order.created_at < :endDateTime
            GROUP BY oi.order_id
        ) item_totals ON item_totals.order_id = o.id
        WHERE o.status = 'DELIVERED'
          AND o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        GROUP BY CAST(o.created_at AS DATE)
        ORDER BY period_start
        """, nativeQuery = true)
    List<Object[]> findDailyRevenueTrend(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT CAST(DATE_TRUNC('month', o.created_at) AS DATE) AS period_start,
               COALESCE(SUM(o.subtotal_amount), 0) AS gmv,
               COALESCE(SUM(o.total_amount), 0) AS delivered_order_value,
               COUNT(*) AS delivered_orders,
               COALESCE(SUM(item_totals.units_sold), 0) AS units_sold
        FROM orders o
        LEFT JOIN (
            SELECT oi.order_id,
                   SUM(oi.quantity) AS units_sold
            FROM order_items oi
            JOIN orders filtered_order ON filtered_order.id = oi.order_id
            WHERE filtered_order.status = 'DELIVERED'
              AND filtered_order.created_at >= :startDateTime
              AND filtered_order.created_at < :endDateTime
            GROUP BY oi.order_id
        ) item_totals ON item_totals.order_id = o.id
        WHERE o.status = 'DELIVERED'
          AND o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        GROUP BY DATE_TRUNC('month', o.created_at)
        ORDER BY period_start
        """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueTrend(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT u.id AS seller_id,
               COALESCE(NULLIF(u.store_name, ''),
                        NULLIF(u.full_name, ''),
                        u.username) AS seller_name,
               COALESCE(SUM(oi.subtotal), 0) AS gmv,
               COUNT(DISTINCT oi.order_id) AS delivered_orders,
               COALESCE(SUM(oi.quantity), 0) AS units_sold
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        JOIN users u ON u.id = oi.seller_id
        WHERE o.status = 'DELIVERED'
          AND o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        GROUP BY u.id, u.store_name, u.full_name, u.username
        ORDER BY gmv DESC, u.id
        LIMIT :topLimit
        """, nativeQuery = true)
    List<Object[]> findTopSellers(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("topLimit") int topLimit
    );

    @Query(value = """
        SELECT p.id AS product_id,
               p.name AS product_name,
               u.id AS seller_id,
               COALESCE(NULLIF(u.store_name, ''),
                        NULLIF(u.full_name, ''),
                        u.username) AS seller_name,
               COALESCE(SUM(oi.subtotal), 0) AS gmv,
               COUNT(DISTINCT oi.order_id) AS delivered_orders,
               COALESCE(SUM(oi.quantity), 0) AS units_sold
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        JOIN products p ON p.id = oi.product_id
        JOIN users u ON u.id = oi.seller_id
        WHERE o.status = 'DELIVERED'
          AND o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        GROUP BY p.id, p.name, u.id, u.store_name, u.full_name, u.username
        ORDER BY gmv DESC, p.id
        LIMIT :topLimit
        """, nativeQuery = true)
    List<Object[]> findTopProducts(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("topLimit") int topLimit
    );

    @Query(value = """
        SELECT c.id AS category_id,
               COALESCE(c.name, 'Uncategorized') AS category_name,
               COALESCE(SUM(oi.subtotal), 0) AS gmv,
               COUNT(DISTINCT oi.order_id) AS delivered_orders,
               COALESCE(SUM(oi.quantity), 0) AS units_sold
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        JOIN products p ON p.id = oi.product_id
        LEFT JOIN categories c ON c.id = p.category_id
        WHERE o.status = 'DELIVERED'
          AND o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        GROUP BY c.id, c.name
        ORDER BY gmv DESC, category_name
        LIMIT :topLimit
        """, nativeQuery = true)
    List<Object[]> findTopCategories(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("topLimit") int topLimit
    );

    @Query(value = """
        SELECT CAST(p.payment_method AS TEXT) AS payment_method,
               COUNT(*) AS total_payments,
               COUNT(*) FILTER (WHERE p.status = 'SUCCESS') AS successful_payments,
               COUNT(*) FILTER (WHERE p.status = 'PENDING') AS pending_payments,
               COUNT(*) FILTER (WHERE p.status = 'FAILED') AS failed_payments,
               COALESCE(SUM(p.amount)
                            FILTER (WHERE p.status = 'SUCCESS'), 0) AS successful_amount
        FROM payments p
        WHERE p.created_at >= :startDateTime
          AND p.created_at < :endDateTime
        GROUP BY p.payment_method
        ORDER BY successful_amount DESC, payment_method
        """, nativeQuery = true)
    List<Object[]> findPaymentMethodDistribution(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT COALESCE(SUM(p.amount), 0)
        FROM payments p
        WHERE p.status = 'SUCCESS'
          AND p.paid_at >= :startDateTime
          AND p.paid_at < :endDateTime
        """, nativeQuery = true)
    BigDecimal findSuccessfulPaymentAmount(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT COUNT(*) FILTER (WHERE r.name = 'SELLER') AS total_sellers,
               COUNT(*) FILTER (
                   WHERE r.name = 'SELLER' AND u.status = 'ACTIVE'
               ) AS active_seller_accounts,
               COUNT(*) FILTER (
                   WHERE r.name = 'SELLER'
                     AND u.created_at >= :startDateTime
                     AND u.created_at < :endDateTime
               ) AS new_sellers,
               COUNT(*) FILTER (WHERE r.name = 'CUSTOMER') AS total_customers,
               COUNT(*) FILTER (
                   WHERE r.name = 'CUSTOMER' AND u.status = 'ACTIVE'
               ) AS active_customer_accounts,
               COUNT(*) FILTER (
                   WHERE r.name = 'CUSTOMER'
                     AND u.created_at >= :startDateTime
                     AND u.created_at < :endDateTime
               ) AS new_customers
        FROM users u
        JOIN roles r ON r.id = u.role_id
        WHERE u.deleted_at IS NULL
          AND r.deleted_at IS NULL
        """, nativeQuery = true)
    Object[] findUserActivity(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT COUNT(*) AS total_products,
               COUNT(*) FILTER (WHERE p.status = 'ACTIVE') AS active_products,
               COUNT(*) FILTER (WHERE p.status = 'INACTIVE') AS inactive_products,
               COUNT(*) FILTER (WHERE p.status = 'OUT_OF_STOCK') AS out_of_stock_products,
               COUNT(*) FILTER (
                   WHERE p.created_at >= :startDateTime
                     AND p.created_at < :endDateTime
               ) AS new_products,
               COUNT(*) FILTER (WHERE p.category_id IS NULL) AS uncategorized_products
        FROM products p
        WHERE p.deleted_at IS NULL
        """, nativeQuery = true)
    Object[] findProductActivity(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM categories c
        WHERE c.deleted_at IS NULL
        """, nativeQuery = true)
    long countActiveCategories();

    @Query(value = """
        WITH activity AS (
            SELECT CAST(u.created_at AS DATE) AS period_start,
                   COUNT(*) FILTER (WHERE r.name = 'SELLER') AS new_sellers,
                   COUNT(*) FILTER (WHERE r.name = 'CUSTOMER') AS new_customers,
                   0::BIGINT AS new_products
            FROM users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.deleted_at IS NULL
              AND r.deleted_at IS NULL
              AND u.created_at >= :startDateTime
              AND u.created_at < :endDateTime
            GROUP BY CAST(u.created_at AS DATE)

            UNION ALL

            SELECT CAST(p.created_at AS DATE) AS period_start,
                   0::BIGINT AS new_sellers,
                   0::BIGINT AS new_customers,
                   COUNT(*) AS new_products
            FROM products p
            WHERE p.deleted_at IS NULL
              AND p.created_at >= :startDateTime
              AND p.created_at < :endDateTime
            GROUP BY CAST(p.created_at AS DATE)
        )
        SELECT period_start,
               SUM(new_sellers) AS new_sellers,
               SUM(new_customers) AS new_customers,
               SUM(new_products) AS new_products
        FROM activity
        GROUP BY period_start
        ORDER BY period_start
        """, nativeQuery = true)
    List<Object[]> findDailyActivityTrend(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = """
        WITH activity AS (
            SELECT CAST(DATE_TRUNC('month', u.created_at) AS DATE) AS period_start,
                   COUNT(*) FILTER (WHERE r.name = 'SELLER') AS new_sellers,
                   COUNT(*) FILTER (WHERE r.name = 'CUSTOMER') AS new_customers,
                   0::BIGINT AS new_products
            FROM users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.deleted_at IS NULL
              AND r.deleted_at IS NULL
              AND u.created_at >= :startDateTime
              AND u.created_at < :endDateTime
            GROUP BY DATE_TRUNC('month', u.created_at)

            UNION ALL

            SELECT CAST(DATE_TRUNC('month', p.created_at) AS DATE) AS period_start,
                   0::BIGINT AS new_sellers,
                   0::BIGINT AS new_customers,
                   COUNT(*) AS new_products
            FROM products p
            WHERE p.deleted_at IS NULL
              AND p.created_at >= :startDateTime
              AND p.created_at < :endDateTime
            GROUP BY DATE_TRUNC('month', p.created_at)
        )
        SELECT period_start,
               SUM(new_sellers) AS new_sellers,
               SUM(new_customers) AS new_customers,
               SUM(new_products) AS new_products
        FROM activity
        GROUP BY period_start
        ORDER BY period_start
        """, nativeQuery = true)
    List<Object[]> findMonthlyActivityTrend(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );
}

package com.example.secdsp.modules.order.repository;

import com.example.secdsp.modules.order.entity.OrderItem;
import com.example.secdsp.modules.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderItemRepository
    extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    @Query("""
        select oi from OrderItem oi
        join fetch oi.product
        where oi.order.id in :orderIds
        """)
    List<OrderItem> findByOrder_IdIn(@Param("orderIds") Collection<Long> orderIds);

    long countBySeller_IdAndOrder_Status(
        Long sellerId,
        OrderStatus status
    );

    List<OrderItem> findTop5BySeller_IdOrderByOrder_CreatedAtDesc(Long sellerId);

    boolean existsByOrder_User_IdAndProduct_IdAndOrder_Status(
        Long userId,
        Long productId,
        OrderStatus status
    );

    @Query("""
        select sum(oi.subtotal)
        from OrderItem oi
        where oi.seller.id = :sellerId
        and oi.order.status = 'DELIVERED'
        """)
    BigDecimal calculateSellerRevenue(Long sellerId);

    @Query("""
        select count(distinct oi.order.id)
        from OrderItem oi
        where oi.seller.id = :sellerId
        and oi.order.status = 'DELIVERED'
        """)
    long countCompletedOrdersBySeller(Long sellerId);

    @Query(value = """
        SELECT to_char(o.created_at, 'YYYY-MM') AS month,
               COALESCE(SUM(oi.subtotal), 0)
        FROM order_items oi
        INNER JOIN orders o ON o.id = oi.order_id
        WHERE oi.seller_id = :sellerId
          AND o.status = 'DELIVERED'
        GROUP BY to_char(o.created_at, 'YYYY-MM')
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> calculateMonthlyRevenue(Long sellerId);

    @Query("""
        select p.id,
               p.name,
               sum(oi.quantity),
               sum(oi.subtotal)
        from OrderItem oi
        join oi.product p
        where oi.seller.id = :sellerId
          and oi.order.status = 'DELIVERED'
        group by p.id, p.name
        order by sum(oi.quantity) desc
        """)
    List<Object[]> findTopSellingProducts(Long sellerId);

    @Query(value = """
        SELECT CAST(o.created_at AS date) AS day,
               COALESCE(SUM(oi.quantity), 0)
        FROM order_items oi
        INNER JOIN orders o ON o.id = oi.order_id
        WHERE oi.product_id = :productId
          AND oi.seller_id = :sellerId
          AND o.status IN ('PAID', 'PROCESSING', 'SHIPPING', 'DELIVERED')
          AND o.created_at >= CURRENT_DATE - CAST(:historyDays AS integer)
        GROUP BY CAST(o.created_at AS date)
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> findDailySoldQuantity(
        @Param("sellerId") Long sellerId,
        @Param("productId") Long productId,
        @Param("historyDays") int historyDays
    );

    @Query(value = """
        SELECT oi.product_id,
               oi.product_name_at_purchase,
               COALESCE(SUM(oi.quantity), 0) AS qty,
               COALESCE(SUM(oi.subtotal), 0) AS revenue,
               COALESCE(AVG(oi.unit_price_at_purchase), 0) AS avg_price
        FROM order_items oi
        INNER JOIN orders o ON o.id = oi.order_id
        WHERE oi.seller_id = :sellerId
          AND o.status IN ('PAID', 'PROCESSING', 'SHIPPING', 'DELIVERED')
          AND o.created_at >= CURRENT_DATE - CAST(:days AS integer)
        GROUP BY oi.product_id, oi.product_name_at_purchase
        ORDER BY qty DESC
        """, nativeQuery = true)
    List<Object[]> findProductSalesStats(
        @Param("sellerId") Long sellerId,
        @Param("days") int days
    );

    @Query(value = """
        SELECT o.id AS order_id,
               o.created_at,
               o.status::text,
               o.total_amount,
               oi.product_id,
               oi.product_name_at_purchase,
               oi.quantity,
               oi.unit_price_at_purchase,
               oi.subtotal,
               oi.seller_id
        FROM order_items oi
        INNER JOIN orders o ON o.id = oi.order_id
        WHERE oi.seller_id = :sellerId
        ORDER BY o.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findPowerBiSalesRowsBySeller(
        @Param("sellerId") Long sellerId,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT o.id AS order_id,
               o.created_at,
               o.status::text,
               o.total_amount,
               oi.product_id,
               oi.product_name_at_purchase,
               oi.quantity,
               oi.unit_price_at_purchase,
               oi.subtotal,
               oi.seller_id
        FROM order_items oi
        INNER JOIN orders o ON o.id = oi.order_id
        ORDER BY o.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findPowerBiSalesRowsAll(@Param("limit") int limit);

    @Query(value = """
        SELECT CAST(o.created_at AS DATE) AS sale_date,
               SUM(oi.quantity) AS quantity_sold
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        WHERE oi.product_id = :productId
          AND o.status = 'DELIVERED'
          AND o.created_at >= :startDateTime
          AND o.created_at < :endDateTime
        GROUP BY CAST(o.created_at AS DATE)
        ORDER BY sale_date
        """, nativeQuery = true)
    List<Object[]> findCompletedDailySalesByProduct(
        Long productId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    );

    @Query(value = """
        SELECT CAST(MIN(o.created_at) AS DATE)
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        WHERE oi.product_id = :productId
          AND o.status = 'DELIVERED'
        """, nativeQuery = true)
    LocalDate findFirstCompletedSaleDateByProduct(Long productId);
}
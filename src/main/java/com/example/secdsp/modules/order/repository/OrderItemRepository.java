package com.example.secdsp.modules.order.repository;

import com.example.secdsp.modules.order.entity.OrderItem;
import com.example.secdsp.modules.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository
    extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    long countBySeller_IdAndOrder_Status(
        Long sellerId,
        OrderStatus status
    );

    List<OrderItem> findTop5BySeller_IdOrderByOrder_CreatedAtDesc(Long sellerId);

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

    @Query("""
        select to_char(oi.order.createdAt, 'YYYY-MM') as month,
               sum(oi.subtotal)
        from OrderItem oi
        where oi.seller.id = :sellerId
        and oi.order.status = 'DELIVERED'
        group by month
        order by month
        """)
    List<Object[]> calculateMonthlyRevenue(Long sellerId);

    @Query("""
        select oi.product.id,
               oi.productNameAtPurchase,
               sum(oi.quantity),
               sum(oi.subtotal)
        from OrderItem oi
        where oi.seller.id = :sellerId
        and oi.order.status = 'DELIVERED'
        group by oi.product.id, oi.productNameAtPurchase
        order by sum(oi.quantity) desc
        """)
    List<Object[]> findTopSellingProducts(Long sellerId);

    boolean existsByOrder_User_IdAndProduct_IdAndOrder_Status(
        Long userId,
        Long productId,
        OrderStatus status
    );

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

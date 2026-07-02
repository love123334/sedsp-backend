package com.example.secdsp.modules.order.repository;

import com.example.secdsp.modules.order.entity.OrderItem;
import com.example.secdsp.modules.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
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
}
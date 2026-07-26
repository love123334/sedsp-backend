package com.example.secdsp.modules.order.repository;

import com.example.secdsp.modules.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository
    extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items"})
    Page<Order> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "user"})
    @Query("""
        select distinct o from Order o
        join o.items i
        where i.seller.id = :sellerId
        """)
    Page<Order> findDistinctBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
}
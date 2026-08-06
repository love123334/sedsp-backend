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

    /**
     * Prefer EXISTS over DISTINCT + bag EntityGraph: the old pattern forced Hibernate to
     * hydrate the full join result in memory before paging, which timed out / returned
     * empty pages after large DSS demo seeds. Items are loaded in the service layer.
     */
    @Query(
        value = """
            select o from Order o
            where exists (
                select 1 from OrderItem i
                where i.order = o and i.seller.id = :sellerId
            )
            """,
        countQuery = """
            select count(o) from Order o
            where exists (
                select 1 from OrderItem i
                where i.order = o and i.seller.id = :sellerId
            )
            """
    )
    Page<Order> findDistinctBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
}
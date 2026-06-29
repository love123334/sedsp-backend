package com.example.secdsp.modules.order.repository;

import com.example.secdsp.modules.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository
    extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items"})
    Page<Order> findByUser_Id(Long userId, Pageable pageable);
}
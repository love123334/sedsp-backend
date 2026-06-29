package com.example.secdsp.modules.cart.repository;

import com.example.secdsp.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCart_IdAndProduct_Id(
        Long cartId,
        Long productId
    );

    @EntityGraph(attributePaths = {"product"})
    List<CartItem> findByCart_Id(Long cartId);

    boolean existsByCart_IdAndProduct_Id(
        Long cartId,
        Long productId
    );

    void deleteAllByCart_Id(Long cartId);
}
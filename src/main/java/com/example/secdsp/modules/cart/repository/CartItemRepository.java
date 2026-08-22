package com.example.secdsp.modules.cart.repository;

import com.example.secdsp.modules.cart.entity.CartItem;
import com.example.secdsp.modules.cart.repository.projection.CartProductQtyRow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCart_IdAndProduct_Id(
        Long cartId,
        Long productId
    );

    @Query(value = """
        SELECT * FROM cart_items
        WHERE cart_id = :cartId AND product_id = :productId
        ORDER BY id DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<CartItem> findIncludingDeleted(
        @Param("cartId") Long cartId,
        @Param("productId") Long productId
    );

    @Query("""
        select ci.product.id as productId, ci.quantity as quantity
        from CartItem ci
        where ci.cart.id = :cartId
        """)
    List<CartProductQtyRow> findProductQtyRowsByCartId(@Param("cartId") Long cartId);

    @Query("""
        select ci from CartItem ci
        join fetch ci.product p
        left join fetch p.seller
        where ci.cart.id = :cartId
        """)
    List<CartItem> findByCartIdWithProduct(@Param("cartId") Long cartId);

    @EntityGraph(attributePaths = {"product", "product.seller"})
    List<CartItem> findByCart_Id(Long cartId);

    boolean existsByCart_IdAndProduct_Id(
        Long cartId,
        Long productId
    );

    void deleteAllByCart_Id(Long cartId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM cart_items WHERE id = :id", nativeQuery = true)
    void hardDeleteById(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM cart_items WHERE cart_id = :cartId", nativeQuery = true)
    void hardDeleteAllByCartId(@Param("cartId") Long cartId);
}
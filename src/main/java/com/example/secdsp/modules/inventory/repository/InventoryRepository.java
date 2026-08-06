package com.example.secdsp.modules.inventory.repository;

import com.example.secdsp.modules.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProduct_IdForUpdate(@Param("productId") Long productId);

    Optional<Inventory> findByProduct_Id(Long productId);

    @Query("""
        select i from Inventory i
        where i.product.id in :productIds
        """)
    List<Inventory> findByProduct_IdIn(@Param("productIds") Collection<Long> productIds);

    @Query("""
        select count(i)
        from Inventory i
        join i.product p
        where p.seller.id = :sellerId
        and i.availableQuantity <= 5
        """)
    long countLowStockBySeller(Long sellerId);

    @Query("""
        select count(i)
        from Inventory i
        join i.product p
        where p.seller.id = :sellerId
        and i.availableQuantity = 0
        """)
    long countOutOfStockBySeller(Long sellerId);

    @Query("""
        select i
        from Inventory i
        join i.product p
        where p.seller.id = :sellerId
        and i.availableQuantity <= 5
        """)
    List<Inventory> findLowStockProductsBySeller(Long sellerId);
}
package com.example.secdsp.modules.inventory.repository;

import com.example.secdsp.modules.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProduct_Id(Long productId);

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
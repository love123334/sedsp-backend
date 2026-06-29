package com.example.secdsp.modules.inventory.repository;

import com.example.secdsp.modules.inventory.entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    Page<InventoryLog> findByProduct_Id(
        Long productId,
        Pageable pageable
    );
}
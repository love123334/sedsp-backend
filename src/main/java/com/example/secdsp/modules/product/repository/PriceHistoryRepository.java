package com.example.secdsp.modules.product.repository;

import com.example.secdsp.modules.product.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository
    extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProduct_IdOrderByChangedAtDesc(Long productId);

}

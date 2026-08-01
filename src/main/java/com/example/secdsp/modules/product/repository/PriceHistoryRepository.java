package com.example.secdsp.modules.product.repository;

import com.example.secdsp.modules.product.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PriceHistoryRepository
    extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProduct_IdOrderByChangedAtDesc(Long productId);

    @Query("""
        select ph
        from PriceHistory ph
        where ph.product.id = :productId
          and ph.changedAt >= :startDateTime
          and ph.changedAt < :endDateTime
        order by ph.changedAt
        """)
    List<PriceHistory> findByProductAndDateRange(
        Long productId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    );
}

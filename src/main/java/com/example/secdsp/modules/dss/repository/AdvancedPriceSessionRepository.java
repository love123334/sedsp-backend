package com.example.secdsp.modules.dss.repository;

import com.example.secdsp.modules.dss.entity.AdvancedPriceSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AdvancedPriceSessionRepository
    extends JpaRepository<AdvancedPriceSession, Long> {

    Optional<AdvancedPriceSession> findByIdAndSellerId(Long id, Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select priceSession
        from AdvancedPriceSession priceSession
        where priceSession.id = :id
          and priceSession.sellerId = :sellerId
        """)
    Optional<AdvancedPriceSession> findOwnedByIdForUpdate(
        Long id,
        Long sellerId
    );
}

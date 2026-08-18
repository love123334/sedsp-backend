package com.example.secdsp.modules.dss.repository;

import com.example.secdsp.modules.dss.entity.AdvancedPriceScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AdvancedPriceScenarioRepository
    extends JpaRepository<AdvancedPriceScenario, Long> {

    List<AdvancedPriceScenario> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    Optional<AdvancedPriceScenario> findByIdAndSessionId(
        Long id,
        Long sessionId
    );

    boolean existsBySessionIdAndPriceChangePercent(
        Long sessionId,
        BigDecimal priceChangePercent
    );
}


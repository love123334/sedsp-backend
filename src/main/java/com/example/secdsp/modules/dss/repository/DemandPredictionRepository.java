package com.example.secdsp.modules.dss.repository;

import com.example.secdsp.modules.dss.entity.DemandPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DemandPredictionRepository
    extends JpaRepository<DemandPrediction, Long> {

    Optional<DemandPrediction>
    findTopByProduct_IdOrderByCreatedAtDesc(Long productId);
}

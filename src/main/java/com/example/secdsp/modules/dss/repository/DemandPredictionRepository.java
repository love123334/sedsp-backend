package com.example.secdsp.modules.dss.repository;

import com.example.secdsp.modules.dss.entity.DemandPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandPredictionRepository
    extends JpaRepository<DemandPrediction, Long> {
}

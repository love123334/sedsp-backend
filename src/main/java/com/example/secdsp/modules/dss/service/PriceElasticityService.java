package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.internal.PriceElasticitySnapshot;

import java.time.LocalDate;

public interface PriceElasticityService {

    PriceElasticitySnapshot analyze(
        Long productId,
        LocalDate fromDate,
        LocalDate toDate
    );

    PriceElasticitySnapshot analyzeAllHistory(Long productId);
}

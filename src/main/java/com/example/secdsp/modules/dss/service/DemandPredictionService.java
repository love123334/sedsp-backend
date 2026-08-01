package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.request.GenerateDemandPredictionRequest;
import com.example.secdsp.modules.dss.dto.response.DemandPredictionResponse;

public interface DemandPredictionService {

    DemandPredictionResponse generatePrediction(
        GenerateDemandPredictionRequest request
    );

    double predictDemand(Long productId, int simulationPeriod);
}

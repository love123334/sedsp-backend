package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.request.CustomPriceScenarioRequest;
import com.example.secdsp.modules.dss.dto.request.GeneratePricePredictionRequest;
import com.example.secdsp.modules.dss.dto.response.CustomPriceScenarioResponse;
import com.example.secdsp.modules.dss.dto.response.PricePredictionResponse;

public interface PricePredictionService {

    PricePredictionResponse generatePrediction(
        GeneratePricePredictionRequest request
    );

    CustomPriceScenarioResponse evaluateCustomPriceScenario(
        CustomPriceScenarioRequest request
    );

    double calculateElasticity(Long productId);
}

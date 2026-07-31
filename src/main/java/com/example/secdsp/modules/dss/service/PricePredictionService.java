package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.request.GeneratePricePredictionRequest;
import com.example.secdsp.modules.dss.dto.response.PricePredictionResponse;

public interface PricePredictionService {

    PricePredictionResponse generatePrediction(
        GeneratePricePredictionRequest request
    );
}

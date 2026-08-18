package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.request.CreateAdvancedPriceScenarioRequest;
import com.example.secdsp.modules.dss.dto.request.CreateAdvancedPriceSessionRequest;
import com.example.secdsp.modules.dss.dto.response.AdvancedPriceSessionResponse;
import com.example.secdsp.modules.dss.dto.response.ApplyAdvancedPriceScenarioResponse;

public interface AdvancedPriceAnalysisService {

    AdvancedPriceSessionResponse createSession(
        CreateAdvancedPriceSessionRequest request
    );

    AdvancedPriceSessionResponse getSession(Long sessionId);

    AdvancedPriceSessionResponse createScenario(
        Long sessionId,
        CreateAdvancedPriceScenarioRequest request
    );

    ApplyAdvancedPriceScenarioResponse applyScenario(
        Long sessionId,
        Long scenarioId
    );
}


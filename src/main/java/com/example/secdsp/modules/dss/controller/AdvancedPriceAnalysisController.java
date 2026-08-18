package com.example.secdsp.modules.dss.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.dss.dto.request.CreateAdvancedPriceScenarioRequest;
import com.example.secdsp.modules.dss.dto.request.CreateAdvancedPriceSessionRequest;
import com.example.secdsp.modules.dss.dto.response.AdvancedPriceSessionResponse;
import com.example.secdsp.modules.dss.dto.response.ApplyAdvancedPriceScenarioResponse;
import com.example.secdsp.modules.dss.service.AdvancedPriceAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dss/advanced-price/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class AdvancedPriceAnalysisController {

    private final AdvancedPriceAnalysisService advancedPriceAnalysisService;

    @PostMapping
    public ResponseEntity<BaseResponse<AdvancedPriceSessionResponse>>
    createSession(
        @Valid @RequestBody CreateAdvancedPriceSessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            BaseResponse.success(
                "Tạo phiên gợi ý giá nâng cao thành công.",
                advancedPriceAnalysisService.createSession(request)
            )
        );
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<BaseResponse<AdvancedPriceSessionResponse>>
    getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(BaseResponse.success(
            advancedPriceAnalysisService.getSession(sessionId)
        ));
    }

    @PostMapping("/{sessionId}/scenarios")
    public ResponseEntity<BaseResponse<AdvancedPriceSessionResponse>>
    createScenario(
        @PathVariable Long sessionId,
        @Valid @RequestBody CreateAdvancedPriceScenarioRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            BaseResponse.success(
                "Tạo kịch bản giá thành công.",
                advancedPriceAnalysisService.createScenario(sessionId, request)
            )
        );
    }

    @PostMapping("/{sessionId}/scenarios/{scenarioId}/apply")
    public ResponseEntity<BaseResponse<ApplyAdvancedPriceScenarioResponse>>
    applyScenario(
        @PathVariable Long sessionId,
        @PathVariable Long scenarioId
    ) {
        return ResponseEntity.ok(BaseResponse.success(
            "Áp dụng giá mới thành công.",
            advancedPriceAnalysisService.applyScenario(sessionId, scenarioId)
        ));
    }
}


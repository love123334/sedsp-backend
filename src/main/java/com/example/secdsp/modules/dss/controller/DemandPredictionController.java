package com.example.secdsp.modules.dss.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.dss.dto.request.GenerateDemandPredictionRequest;
import com.example.secdsp.modules.dss.dto.response.DemandPredictionResponse;
import com.example.secdsp.modules.dss.service.DemandPredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dss/demand-predictions")
@RequiredArgsConstructor
public class DemandPredictionController {

    private final DemandPredictionService demandPredictionService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<BaseResponse<DemandPredictionResponse>>
    generatePrediction(
        @Valid @RequestBody GenerateDemandPredictionRequest request
    ) {
        return new ResponseEntity<>(
            BaseResponse.success(
                "Tạo dự báo nhu cầu thành công.",
                demandPredictionService.generatePrediction(request)
            ),
            HttpStatus.CREATED
        );
    }
}

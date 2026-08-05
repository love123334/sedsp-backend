package com.example.secdsp.modules.dss.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.dss.dto.request.GeneratePricePredictionRequest;
import com.example.secdsp.modules.dss.dto.response.PricePredictionResponse;
import com.example.secdsp.modules.dss.service.PricePredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dss/price-predictions")
@RequiredArgsConstructor
public class PricePredictionController {

    private final PricePredictionService pricePredictionService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<BaseResponse<PricePredictionResponse>>
    generatePrediction(
        @Valid @RequestBody GeneratePricePredictionRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                "Tạo khuyến nghị giá thành công.",
                pricePredictionService.generatePrediction(request)
            )
        );
    }
}

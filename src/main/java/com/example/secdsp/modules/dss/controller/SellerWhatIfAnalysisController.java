package com.example.secdsp.modules.dss.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.dss.dto.request.SellerDiscountAnalysisRequest;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import com.example.secdsp.modules.dss.service.SellerWhatIfAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dss/what-if/seller")
@RequiredArgsConstructor
public class SellerWhatIfAnalysisController {

    private final SellerWhatIfAnalysisService sellerWhatIfAnalysisService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<SellerDiscountAnalysisResponse>>
    analyzeDiscount(
        @Valid @RequestBody SellerDiscountAnalysisRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                "Phân tích kịch bản giảm giá thành công.",
                sellerWhatIfAnalysisService.analyzeDiscount(request)
            )
        );
    }
}

package com.example.secdsp.modules.dss.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.dss.dto.request.SalesQuantityTargetRequest;
import com.example.secdsp.modules.dss.dto.request.SellerDiscountAnalysisRequest;
import com.example.secdsp.modules.dss.dto.request.TargetProfitAnalysisRequest;
import com.example.secdsp.modules.dss.dto.response.SalesQuantityTargetResponse;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import com.example.secdsp.modules.dss.dto.response.TargetProfitAnalysisResponse;
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
@RequestMapping({"/api/dss/what-if/seller", "/api/v1/dss/what-if/seller"})
@RequiredArgsConstructor
public class SellerWhatIfAnalysisController {

    private final SellerWhatIfAnalysisService sellerWhatIfAnalysisService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<BaseResponse<SellerDiscountAnalysisResponse>>
    analyzeDiscount(
        @Valid @RequestBody SellerDiscountAnalysisRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                "Phân tích kịch bản giảm giá thành công.",
                sellerWhatIfAnalysisService.analyzeDiscount(request)
            )
        );
    }

    @PostMapping("/target-profit")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<BaseResponse<TargetProfitAnalysisResponse>>
    analyzeTargetProfit(
        @Valid @RequestBody TargetProfitAnalysisRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                "Phân tích mục tiêu lợi nhuận thành công.",
                sellerWhatIfAnalysisService.analyzeTargetProfit(request)
            )
        );
    }

    @PostMapping("/sales-quantity-target")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<BaseResponse<SalesQuantityTargetResponse>>
    analyzeSalesQuantityTarget(
        @Valid @RequestBody SalesQuantityTargetRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                "Phân tích mục tiêu số lượng bán thành công.",
                sellerWhatIfAnalysisService.analyzeSalesQuantityTarget(request)
            )
        );
    }
}

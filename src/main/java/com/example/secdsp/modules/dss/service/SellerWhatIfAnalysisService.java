package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.request.SalesQuantityTargetRequest;
import com.example.secdsp.modules.dss.dto.request.SellerDiscountAnalysisRequest;
import com.example.secdsp.modules.dss.dto.request.TargetProfitAnalysisRequest;
import com.example.secdsp.modules.dss.dto.response.SalesQuantityTargetResponse;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import com.example.secdsp.modules.dss.dto.response.TargetProfitAnalysisResponse;

public interface SellerWhatIfAnalysisService {

    SellerDiscountAnalysisResponse analyzeDiscount(
        SellerDiscountAnalysisRequest request
    );

    TargetProfitAnalysisResponse analyzeTargetProfit(
        TargetProfitAnalysisRequest request
    );

    SalesQuantityTargetResponse analyzeSalesQuantityTarget(
        SalesQuantityTargetRequest request
    );
}

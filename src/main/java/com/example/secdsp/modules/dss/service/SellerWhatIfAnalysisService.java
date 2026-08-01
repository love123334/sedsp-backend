package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.request.SellerDiscountAnalysisRequest;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;

public interface SellerWhatIfAnalysisService {

    SellerDiscountAnalysisResponse analyzeDiscount(
        SellerDiscountAnalysisRequest request
    );
}

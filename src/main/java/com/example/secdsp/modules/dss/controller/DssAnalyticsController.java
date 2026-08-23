package com.example.secdsp.modules.dss.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.dss.dto.DemandForecastResponse;
import com.example.secdsp.modules.dss.dto.DssInsightPlanResponse;
import com.example.secdsp.modules.dss.dto.InventoryRecommendationResponse;
import com.example.secdsp.modules.dss.dto.PriceRecommendationResponse;
import com.example.secdsp.modules.dss.service.DssAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DssAnalyticsController {

    private final DssAnalyticsService dssAnalyticsService;

    @GetMapping("/dss/demand/{productId}")
    @PreAuthorize("hasAnyRole('SELLER','MANAGER','ADMIN')")
    public ResponseEntity<BaseResponse<DemandForecastResponse>> demand(
        @PathVariable Long productId,
        @RequestParam(defaultValue = "90") int historyDays,
        @RequestParam(defaultValue = "30") int forecastDays
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                dssAnalyticsService.forecastDemand(productId, historyDays, forecastDays)
            )
        );
    }

    @GetMapping("/dss/price/{productId}")
    @PreAuthorize("hasAnyRole('SELLER','MANAGER','ADMIN')")
    public ResponseEntity<BaseResponse<PriceRecommendationResponse>> price(
        @PathVariable Long productId,
        @RequestParam(defaultValue = "30") int lookbackDays
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                dssAnalyticsService.recommendPrice(productId, lookbackDays)
            )
        );
    }

    @GetMapping("/dss/inventory")
    @PreAuthorize("hasAnyRole('SELLER','MANAGER','ADMIN')")
    public ResponseEntity<BaseResponse<InventoryRecommendationResponse>> inventory(
        @RequestParam(required = false) Long productId,
        @RequestParam(defaultValue = "14") int planningDays
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                dssAnalyticsService.recommendInventory(productId, planningDays)
            )
        );
    }

    /** Power BI brain: metrics from web + AI commentary / optional embed */
    @GetMapping("/dss/insights/plan")
    @PreAuthorize("hasAnyRole('SELLER','MANAGER','ADMIN')")
    public ResponseEntity<BaseResponse<DssInsightPlanResponse>> insightPlan() {
        return ResponseEntity.ok(
            BaseResponse.success(dssAnalyticsService.buildInsightPlan())
        );
    }

    @GetMapping("/analytics/powerbi/sales")
    @PreAuthorize("hasAnyRole('SELLER','MANAGER','ADMIN')")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> powerBiSales(
        @RequestParam(defaultValue = "1000") int limit
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(dssAnalyticsService.powerBiSalesFeed(limit))
        );
    }

    @GetMapping("/dss/business-health")
    @PreAuthorize("hasAnyRole('SELLER','MANAGER','ADMIN')")
    public ResponseEntity<BaseResponse<com.example.secdsp.modules.dss.dto.BusinessHealthResponse>> businessHealth() {
        return ResponseEntity.ok(
            BaseResponse.success(dssAnalyticsService.getBusinessHealthScore())
        );
    }
}



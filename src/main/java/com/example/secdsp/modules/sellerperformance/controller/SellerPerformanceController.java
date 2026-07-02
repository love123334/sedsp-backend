package com.example.secdsp.modules.sellerperformance.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.sellerperformance.dto.response.SalesPerformanceResponse;
import com.example.secdsp.modules.sellerperformance.service.SellerPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/sales-performance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerPerformanceController {

    private final SellerPerformanceService service;

    @GetMapping
    public ResponseEntity<ApiResponse<SalesPerformanceResponse>>
    getPerformance() {

        return ResponseEntity.ok(
            ApiResponse.success(
                service.getPerformance()
            )
        );
    }
}

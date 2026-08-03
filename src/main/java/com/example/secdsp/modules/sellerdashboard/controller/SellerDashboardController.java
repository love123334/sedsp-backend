package com.example.secdsp.modules.sellerdashboard.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.sellerdashboard.dto.SellerDashboardResponse;
import com.example.secdsp.modules.sellerdashboard.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerDashboardController {

    private final SellerDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<BaseResponse<SellerDashboardResponse>>
    getDashboard() {

        return ResponseEntity.ok(
            BaseResponse.success(
                dashboardService.getDashboard()
            )
        );
    }
}

package com.example.secdsp.modules.platformrevenue.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.platformrevenue.dto.request.PlatformRevenueDashboardRequest;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse;
import com.example.secdsp.modules.platformrevenue.service.PlatformRevenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/platform-revenue/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class PlatformRevenueController {

    private final PlatformRevenueService platformRevenueService;

    @GetMapping
    public ResponseEntity<BaseResponse<PlatformRevenueDashboardResponse>>
    getDashboard(
        @Valid @ModelAttribute PlatformRevenueDashboardRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                "Lấy báo cáo doanh thu toàn sàn thành công.",
                platformRevenueService.getDashboard(request)
            )
        );
    }
}

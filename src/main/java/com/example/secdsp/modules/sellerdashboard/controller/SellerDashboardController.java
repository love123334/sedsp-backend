package com.example.secdsp.modules.sellerdashboard.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.sellerdashboard.dto.SellerDashboardResponse;
import com.example.secdsp.modules.sellerdashboard.service.SellerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
    name = "Seller Dashboard",
    description = "APIs for retrieving seller dashboard information."
)
public class SellerDashboardController {

    private final SellerDashboardService dashboardService;

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get seller dashboard",
        description = "Retrieve dashboard statistics, revenue, orders, inventory, ratings and actionable recommendations for the authenticated seller."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Seller dashboard retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
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

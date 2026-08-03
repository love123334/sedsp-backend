package com.example.secdsp.modules.sellerperformance.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.sellerperformance.dto.response.SalesPerformanceResponse;
import com.example.secdsp.modules.sellerperformance.service.SellerPerformanceService;
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
@RequestMapping("/api/v1/seller/sales-performance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(
    name = "Seller Performance",
    description = "APIs for retrieving seller sales performance statistics."
)
public class SellerPerformanceController {

    private final SellerPerformanceService service;

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get sales performance",
        description = "Retrieve sales performance statistics for the authenticated seller."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sales performance retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @GetMapping
    public ResponseEntity<BaseResponse<SalesPerformanceResponse>>
    getPerformance() {

        return ResponseEntity.ok(
            BaseResponse.success(
                service.getPerformance()
            )
        );
    }
}

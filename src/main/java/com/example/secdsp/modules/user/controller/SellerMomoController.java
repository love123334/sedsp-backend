package com.example.secdsp.modules.user.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.user.dto.request.UpdateSellerMomoRequest;
import com.example.secdsp.modules.user.dto.response.SellerMomoPublicResponse;
import com.example.secdsp.modules.user.dto.response.SellerMomoSettingsResponse;
import com.example.secdsp.modules.user.service.SellerMomoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Seller MoMo", description = "Seller MoMo QR transfer settings")
@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerMomoController {

    private final SellerMomoService sellerMomoService;

    @GetMapping("/{sellerId}/momo")
    @Operation(summary = "Public seller MoMo info for checkout")
    public ResponseEntity<BaseResponse<SellerMomoPublicResponse>> getPublicMomo(
        @PathVariable Long sellerId
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(sellerMomoService.getPublicMomo(sellerId))
        );
    }

    @GetMapping("/me/momo")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get authenticated seller MoMo settings")
    public ResponseEntity<BaseResponse<SellerMomoSettingsResponse>> getMyMomo() {
        return ResponseEntity.ok(
            BaseResponse.success(sellerMomoService.getMyMomoSettings())
        );
    }

    @PutMapping("/me/momo")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update authenticated seller MoMo settings")
    public ResponseEntity<BaseResponse<SellerMomoSettingsResponse>> updateMyMomo(
        @Valid @RequestBody UpdateSellerMomoRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                "MoMo settings updated",
                sellerMomoService.updateMyMomoSettings(request)
            )
        );
    }
}

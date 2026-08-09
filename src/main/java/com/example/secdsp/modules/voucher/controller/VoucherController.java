package com.example.secdsp.modules.voucher.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.voucher.dto.ValidateVoucherRequest;
import com.example.secdsp.modules.voucher.dto.ValidateVoucherResponse;
import com.example.secdsp.modules.voucher.dto.VoucherResponse;
import com.example.secdsp.modules.voucher.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/public")
    public ResponseEntity<BaseResponse<List<VoucherResponse>>> listPublic(
        @RequestParam(required = false) Long sellerId
    ) {
        return ResponseEntity.ok(BaseResponse.success(voucherService.listPublicVouchers(sellerId)));
    }

    @PostMapping("/validate")
    public ResponseEntity<BaseResponse<ValidateVoucherResponse>> validate(
        @Valid @RequestBody ValidateVoucherRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(BaseResponse.success(voucherService.validateForCart(userId, request)));
    }
}

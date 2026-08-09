package com.example.secdsp.modules.voucher.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.voucher.dto.CreateVoucherRequestDto;
import com.example.secdsp.modules.voucher.dto.VoucherRequestResponse;
import com.example.secdsp.modules.voucher.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/voucher-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerVoucherRequestController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<VoucherRequestResponse>>> mine() {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(BaseResponse.success(voucherService.listSellerRequests(sellerId)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<VoucherRequestResponse>> create(
        @Valid @RequestBody CreateVoucherRequestDto request
    ) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        VoucherRequestResponse created = voucherService.createSellerRequest(request, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BaseResponse.success("Gửi yêu cầu voucher thành công", created));
    }
}

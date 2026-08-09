package com.example.secdsp.modules.voucher.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.voucher.dto.*;
import com.example.secdsp.modules.voucher.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ManagerVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<VoucherResponse>>> list() {
        return ResponseEntity.ok(BaseResponse.success(voucherService.listManagerVouchers()));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<VoucherResponse>> create(
        @Valid @RequestBody UpsertVoucherRequest request
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        VoucherResponse created = voucherService.createManagerVoucher(request, managerId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BaseResponse.success("Tạo voucher thành công", created));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<BaseResponse<VoucherResponse>> setActive(
        @PathVariable Long id,
        @RequestBody Map<String, Boolean> body
    ) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(
            BaseResponse.success(voucherService.setVoucherActive(id, active))
        );
    }

    @GetMapping("/requests")
    public ResponseEntity<BaseResponse<List<VoucherRequestResponse>>> pendingRequests() {
        return ResponseEntity.ok(BaseResponse.success(voucherService.listPendingRequests()));
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<BaseResponse<VoucherRequestResponse>> approve(
        @PathVariable Long id,
        @RequestBody(required = false) ReviewVoucherRequestDto review
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
            BaseResponse.success(
                "Đã duyệt voucher shop",
                voucherService.approveRequest(id, review, managerId)
            )
        );
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<BaseResponse<VoucherRequestResponse>> reject(
        @PathVariable Long id,
        @RequestBody(required = false) ReviewVoucherRequestDto review
    ) {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
            BaseResponse.success(
                "Đã từ chối yêu cầu voucher",
                voucherService.rejectRequest(id, review, managerId)
            )
        );
    }
}

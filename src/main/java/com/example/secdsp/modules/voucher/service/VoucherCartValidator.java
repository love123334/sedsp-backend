package com.example.secdsp.modules.voucher.service;

import com.example.secdsp.modules.voucher.dto.ValidateVoucherRequest;
import com.example.secdsp.modules.voucher.dto.ValidateVoucherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Validates cart vouchers outside a Spring transaction so caught exceptions
 * never mark the request rollback-only (409 on Railway).
 */
@Service
@RequiredArgsConstructor
public class VoucherCartValidator {

    private final VoucherServiceImpl voucherService;

    public ValidateVoucherResponse validateForCart(Long userId, ValidateVoucherRequest request) {
        return voucherService.validateForCartInternal(userId, request);
    }
}

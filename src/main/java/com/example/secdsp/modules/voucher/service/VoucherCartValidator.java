package com.example.secdsp.modules.voucher.service;

import com.example.secdsp.modules.voucher.dto.ValidateVoucherRequest;
import com.example.secdsp.modules.voucher.dto.ValidateVoucherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates cart vouchers in a read-only transaction so product/seller
 * join-fetch rows stay initialized while pricing lines are built.
 */
@Service
@RequiredArgsConstructor
public class VoucherCartValidator {

    private final VoucherServiceImpl voucherService;

    @Transactional(readOnly = true)
    public ValidateVoucherResponse validateForCart(Long userId, ValidateVoucherRequest request) {
        return voucherService.validateForCartInternal(userId, request);
    }
}

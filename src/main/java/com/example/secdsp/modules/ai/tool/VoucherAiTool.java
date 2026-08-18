package com.example.secdsp.modules.ai.tool;

import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.voucher.dto.ValidateVoucherRequest;
import com.example.secdsp.modules.voucher.dto.ValidateVoucherResponse;
import com.example.secdsp.modules.voucher.dto.VoucherResponse;
import com.example.secdsp.modules.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VoucherAiTool {

    private final VoucherService voucherService;

    public List<VoucherResponse> listPublicVouchers(Long sellerId) {
        return voucherService.listPublicVouchers(sellerId);
    }

    public ValidateVoucherResponse validateVoucher(
        String code,
        List<Long> productIds
    ) {

        Long userId = SecurityUtils.getCurrentUserId();

        ValidateVoucherRequest request =
            new ValidateVoucherRequest();

        request.setCode(code);
        request.setProductIds(productIds);

        return voucherService.validateForCart(userId, request);
    }
}
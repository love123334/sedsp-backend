package com.example.secdsp.modules.voucher.service;

import com.example.secdsp.modules.voucher.dto.*;
import com.example.secdsp.modules.voucher.entity.*;
import com.example.secdsp.modules.cart.entity.CartItem;
import com.example.secdsp.modules.order.entity.Order;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.product.service.ProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

public interface VoucherService {

    VoucherResponse createManagerVoucher(UpsertVoucherRequest request, Long managerId);

    List<VoucherResponse> listManagerVouchers();

    VoucherResponse setVoucherActive(Long id, boolean active);

    VoucherRequestResponse createSellerRequest(CreateVoucherRequestDto request, Long sellerId);

    List<VoucherRequestResponse> listSellerRequests(Long sellerId);

    List<VoucherRequestResponse> listPendingRequests();

    VoucherRequestResponse approveRequest(Long requestId, ReviewVoucherRequestDto review, Long managerId);

    VoucherRequestResponse rejectRequest(Long requestId, ReviewVoucherRequestDto review, Long managerId);

    ValidateVoucherResponse validateForCart(Long userId, ValidateVoucherRequest request);

    List<VoucherResponse> listPublicVouchers(Long sellerId);

    AppliedVoucher applyToOrder(
        Order order,
        String voucherCode,
        List<CartItem> cartItems,
        Long userId
    );

    record AppliedVoucher(Voucher voucher, BigDecimal discountAmount) {}
}

package com.example.secdsp.modules.voucher.service;

import com.example.secdsp.modules.cart.entity.Cart;
import com.example.secdsp.modules.cart.repository.CartItemRepository;
import com.example.secdsp.modules.cart.repository.CartRepository;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.voucher.dto.ValidateVoucherRequest;
import com.example.secdsp.modules.voucher.dto.ValidateVoucherResponse;
import com.example.secdsp.modules.voucher.entity.*;
import com.example.secdsp.modules.voucher.repository.VoucherRepository;
import com.example.secdsp.modules.voucher.repository.VoucherRequestRepository;
import com.example.secdsp.modules.voucher.repository.VoucherUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherValidateServiceTest {

    @Mock
    VoucherRepository voucherRepository;
    @Mock
    VoucherRequestRepository voucherRequestRepository;
    @Mock
    VoucherUsageRepository voucherUsageRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    CartRepository cartRepository;
    @Mock
    CartItemRepository cartItemRepository;

    VoucherServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VoucherServiceImpl(
            voucherRepository,
            voucherRequestRepository,
            voucherUsageRepository,
            productRepository,
            cartRepository,
            cartItemRepository
        );
    }

    @Test
    void validatePlatformVoucher_returnsDiscount() {
        ValidateVoucherRequest request = new ValidateVoucherRequest();
        request.setCode("SEDSP10");
        request.setProductIds(List.of(19L));

        List<Object[]> pricingRows = new ArrayList<>();
        pricingRows.add(new Object[] { 19L, 55L, new BigDecimal("38990000.00") });
        when(productRepository.findPricingRowsByIdIn(List.of(19L)))
            .thenReturn(pricingRows);

        Voucher platform = new Voucher();
        platform.setId(1L);
        platform.setCode("SEDSP10");
        platform.setName("Giảm 10%");
        platform.setDiscountType(VoucherDiscountType.PERCENTAGE);
        platform.setDiscountValue(new BigDecimal("10"));
        platform.setScope(VoucherScope.PLATFORM);
        platform.setAppliesTo(VoucherAppliesTo.ALL_PRODUCTS);
        platform.setMinimumOrderAmount(new BigDecimal("200000"));
        platform.setMaximumDiscountAmount(new BigDecimal("100000"));
        platform.setIsActive(true);
        platform.setUsedCount(0);
        platform.setStartsAt(OffsetDateTime.now().minusDays(1));
        platform.setEndsAt(OffsetDateTime.now().plusDays(30));

        when(voucherRepository.findPlatformByCodeIgnoreCase("SEDSP10"))
            .thenReturn(Optional.of(platform));

        ValidateVoucherResponse res = service.validateForCartInternal(37L, request);

        assertThat(res.valid()).isTrue();
        assertThat(res.discountAmount()).isEqualByComparingTo("100000");
        assertThat(res.code()).isEqualTo("SEDSP10");
    }

    @Test
    void validatePlatformVoucher_acceptsDoublePriceFromJdbc() {
        ValidateVoucherRequest request = new ValidateVoucherRequest();
        request.setCode("SEDSP10");
        request.setProductIds(List.of(19L));

        List<Object[]> pricingRows = new ArrayList<>();
        pricingRows.add(new Object[] { 19L, 55L, Double.valueOf(38990000.00) });
        when(productRepository.findPricingRowsByIdIn(List.of(19L)))
            .thenReturn(pricingRows);

        Voucher platform = new Voucher();
        platform.setId(1L);
        platform.setCode("SEDSP10");
        platform.setName("Giảm 10%");
        platform.setDiscountType(VoucherDiscountType.PERCENTAGE);
        platform.setDiscountValue(new BigDecimal("10"));
        platform.setScope(VoucherScope.PLATFORM);
        platform.setAppliesTo(VoucherAppliesTo.ALL_PRODUCTS);
        platform.setMinimumOrderAmount(new BigDecimal("200000"));
        platform.setMaximumDiscountAmount(new BigDecimal("100000"));
        platform.setIsActive(true);
        platform.setUsedCount(0);
        platform.setStartsAt(OffsetDateTime.now().minusDays(1));
        platform.setEndsAt(OffsetDateTime.now().plusDays(30));

        when(voucherRepository.findPlatformByCodeIgnoreCase("SEDSP10"))
            .thenReturn(Optional.of(platform));

        ValidateVoucherResponse res = service.validateForCartInternal(37L, request);

        assertThat(res.valid()).isTrue();
        assertThat(res.discountAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void validateUsesCartRowsWhenProductIdsMissing() {
        ValidateVoucherRequest request = new ValidateVoucherRequest();
        request.setCode("SEDSP10");

        Cart cart = new Cart();
        cart.setId(4L);
        when(cartRepository.findByUser_Id(37L)).thenReturn(Optional.of(cart));
        List<Object[]> cartRows = new ArrayList<>();
        cartRows.add(new Object[] { 19L, 2 });
        when(cartItemRepository.findProductQtyRowsByCartId(4L))
            .thenReturn(cartRows);

        List<Object[]> pricingRows = new ArrayList<>();
        pricingRows.add(new Object[] { 19L, 55L, new BigDecimal("38990000.00") });
        when(productRepository.findPricingRowsByIdIn(List.of(19L)))
            .thenReturn(pricingRows);

        Voucher platform = new Voucher();
        platform.setId(1L);
        platform.setCode("SEDSP10");
        platform.setName("Giảm 10%");
        platform.setDiscountType(VoucherDiscountType.PERCENTAGE);
        platform.setDiscountValue(new BigDecimal("10"));
        platform.setScope(VoucherScope.PLATFORM);
        platform.setAppliesTo(VoucherAppliesTo.ALL_PRODUCTS);
        platform.setMinimumOrderAmount(new BigDecimal("200000"));
        platform.setMaximumDiscountAmount(new BigDecimal("100000"));
        platform.setIsActive(true);
        platform.setUsedCount(0);
        platform.setStartsAt(OffsetDateTime.now().minusDays(1));
        platform.setEndsAt(OffsetDateTime.now().plusDays(30));

        when(voucherRepository.findPlatformByCodeIgnoreCase("SEDSP10"))
            .thenReturn(Optional.of(platform));

        ValidateVoucherResponse res = service.validateForCartInternal(37L, request);

        assertThat(res.valid()).isTrue();
        assertThat(res.discountAmount()).isEqualByComparingTo("100000");
    }
}

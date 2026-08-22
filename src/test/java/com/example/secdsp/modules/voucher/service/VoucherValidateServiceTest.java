package com.example.secdsp.modules.voucher.service;

import com.example.secdsp.modules.cart.entity.Cart;
import com.example.secdsp.modules.cart.repository.CartItemRepository;
import com.example.secdsp.modules.cart.repository.CartRepository;
import com.example.secdsp.modules.cart.repository.projection.CartProductQtyRow;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.product.repository.projection.ProductPricingRow;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

        ProductPricingRow pricing = pricingRow(19L, 55L, "38990000.00");
        when(productRepository.findPricingRowsByIdIn(List.of(19L)))
            .thenReturn(List.of(pricing));

        Voucher platform = platformVoucher();
        when(voucherRepository.findPlatformByCodeIgnoreCase("SEDSP10"))
            .thenReturn(Optional.of(platform));

        ValidateVoucherResponse res = service.validateForCartInternal(37L, request);

        assertThat(res.valid()).isTrue();
        assertThat(res.discountAmount()).isEqualByComparingTo("100000");
        assertThat(res.code()).isEqualTo("SEDSP10");
    }

    @Test
    void validateUsesCartRowsWhenProductIdsMissing() {
        ValidateVoucherRequest request = new ValidateVoucherRequest();
        request.setCode("SEDSP10");

        Cart cart = new Cart();
        cart.setId(4L);
        when(cartRepository.findByUser_Id(37L)).thenReturn(Optional.of(cart));
        CartProductQtyRow cartRow = cartRow(19L, 2);
        when(cartItemRepository.findProductQtyRowsByCartId(4L))
            .thenReturn(List.of(cartRow));

        ProductPricingRow pricing = pricingRow(19L, 55L, "38990000.00");
        when(productRepository.findPricingRowsByIdIn(List.of(19L)))
            .thenReturn(List.of(pricing));

        when(voucherRepository.findPlatformByCodeIgnoreCase("SEDSP10"))
            .thenReturn(Optional.of(platformVoucher()));

        ValidateVoucherResponse res = service.validateForCartInternal(37L, request);

        assertThat(res.valid()).isTrue();
        assertThat(res.discountAmount()).isEqualByComparingTo("100000");
    }

    private static ProductPricingRow pricingRow(Long id, Long sellerId, String price) {
        ProductPricingRow row = mock(ProductPricingRow.class);
        when(row.getId()).thenReturn(id);
        when(row.getSellerId()).thenReturn(sellerId);
        when(row.getPrice()).thenReturn(new BigDecimal(price));
        return row;
    }

    private static CartProductQtyRow cartRow(Long productId, int qty) {
        CartProductQtyRow row = mock(CartProductQtyRow.class);
        when(row.getProductId()).thenReturn(productId);
        when(row.getQuantity()).thenReturn(qty);
        return row;
    }

    private static Voucher platformVoucher() {
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
        return platform;
    }
}

package com.example.secdsp.modules.voucher.service;

import com.example.secdsp.modules.cart.entity.Cart;
import com.example.secdsp.modules.cart.entity.CartItem;
import com.example.secdsp.modules.cart.repository.CartItemRepository;
import com.example.secdsp.modules.cart.repository.CartRepository;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.user.entity.User;
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

        when(productRepository.findAllWithSellerByIdIn(List.of(19L)))
            .thenReturn(List.of(product(19L, 55L, "38990000.00")));

        when(voucherRepository.findPlatformByCodeIgnoreCase("SEDSP10"))
            .thenReturn(Optional.of(platformVoucher()));

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
        when(cartItemRepository.findByCartIdWithProduct(4L))
            .thenReturn(List.of(cartItem(19L, 55L, "38990000.00", 2)));

        when(productRepository.findAllWithSellerByIdIn(List.of(19L)))
            .thenReturn(List.of(product(19L, 55L, "38990000.00")));

        when(voucherRepository.findPlatformByCodeIgnoreCase("SEDSP10"))
            .thenReturn(Optional.of(platformVoucher()));

        ValidateVoucherResponse res = service.validateForCartInternal(37L, request);

        assertThat(res.valid()).isTrue();
        assertThat(res.discountAmount()).isEqualByComparingTo("100000");
    }

    private static Product product(Long id, Long sellerId, String price) {
        Product product = new Product();
        product.setId(id);
        product.setPrice(new BigDecimal(price));
        if (sellerId != null) {
            User seller = new User();
            seller.setId(sellerId);
            product.setSeller(seller);
        }
        return product;
    }

    private static CartItem cartItem(Long productId, Long sellerId, String price, int qty) {
        CartItem item = new CartItem();
        item.setQuantity(qty);
        item.setProduct(product(productId, sellerId, price));
        return item;
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

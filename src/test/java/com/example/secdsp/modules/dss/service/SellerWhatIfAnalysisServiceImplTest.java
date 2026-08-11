package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.request.SellerDiscountAnalysisRequest;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import com.example.secdsp.modules.dss.repository.DemandPredictionRepository;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.ProductStatus;
import com.example.secdsp.modules.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerWhatIfAnalysisServiceImplTest {

    private static final Long PRODUCT_ID = 15L;
    private static final Long SELLER_ID = 7L;

    @Mock
    ProductService productService;

    @Mock
    DemandPredictionService demandPredictionService;

    @Mock
    PricePredictionService pricePredictionService;

    @Mock
    SellerDiscountProfitCalculator calculator;

    @Mock
    DemandPredictionRepository demandPredictionRepository;

    @InjectMocks
    SellerWhatIfAnalysisServiceImpl service;

    @Test
    void analyzeDiscountUsesDemandAndElasticityServices() {
        SellerDiscountAnalysisRequest request = buildRequest();
        ProductInfo product = buildProduct(SELLER_ID);
        SellerDiscountAnalysisResponse expected =
            SellerDiscountAnalysisResponse.builder()
                .expectedProfit(new BigDecimal("2360.00"))
                .build();

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(product);
        when(demandPredictionRepository
            .findTopByProduct_IdOrderByCreatedAtDesc(PRODUCT_ID))
            .thenReturn(Optional.empty());
        when(demandPredictionService.predictDemand(PRODUCT_ID, 30))
            .thenReturn(100.0);
        when(pricePredictionService.calculateElasticity(PRODUCT_ID))
            .thenReturn(-1.8);
        when(calculator.calculate(
            eq(product.price()),
            eq(product.costPrice()),
            eq(request.getPriceChangePercent()),
            eq(100.0),
            eq(-1.8),
            eq(request.getSimulationPeriod()),
            anyString(),
            anyString()
        )).thenReturn(expected);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId)
                .thenReturn(SELLER_ID);

            SellerDiscountAnalysisResponse actual =
                service.analyzeDiscount(request);

            assertEquals(expected, actual);
        }

        verify(demandPredictionService)
            .predictDemand(PRODUCT_ID, 30);
        verify(pricePredictionService)
            .calculateElasticity(PRODUCT_ID);
    }

    @Test
    void analyzeDiscountRejectsProductOwnedByAnotherSeller() {
        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(buildProduct(99L));

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId)
                .thenReturn(SELLER_ID);

            assertThrows(
                ForbiddenException.class,
                () -> service.analyzeDiscount(buildRequest())
            );
        }
    }

    private SellerDiscountAnalysisRequest buildRequest() {
        SellerDiscountAnalysisRequest request =
            new SellerDiscountAnalysisRequest();
        request.setProductId(PRODUCT_ID);
        request.setPriceChangePercent(new BigDecimal("-10"));
        request.setSimulationPeriod(30);
        return request;
    }

    private ProductInfo buildProduct(Long sellerId) {
        return new ProductInfo(
            PRODUCT_ID,
            sellerId,
            "Wireless Mouse",
            new BigDecimal("100.00"),
            new BigDecimal("70.00"),
            ProductStatus.ACTIVE
        );
    }
}

package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.config.DssProperties;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.request.GeneratePricePredictionRequest;
import com.example.secdsp.modules.dss.dto.response.DssAiInsightResponse;
import com.example.secdsp.modules.dss.dto.response.DssProductContextResponse;
import com.example.secdsp.modules.dss.dto.response.PricePredictionResponse;
import com.example.secdsp.modules.dss.dto.response.PriceScenarioResponse;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.PriceHistoryInfo;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.ProductStatus;
import com.example.secdsp.modules.product.dto.response.PriceHistoryResponse;
import com.example.secdsp.modules.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricePredictionServiceImplTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 7L;
    private static final LocalDate FROM_DATE =
        LocalDate.of(2026, 7, 1);
    private static final LocalDate TO_DATE =
        LocalDate.of(2026, 7, 30);

    @Mock
    ProductService productService;

    @Mock
    OrderService orderService;

    @Mock
    DssProductContextService productContextService;

    @Mock
    DssPredictionInsightService predictionInsightService;

    private DssProperties dssProperties;
    private PricePredictionServiceImpl pricePredictionService;

    @BeforeEach
    void setUp() {
        dssProperties = new DssProperties();
        dssProperties.setIncludeShippingInProfit(false);
        dssProperties.setIncludePlatformFee(false);
        dssProperties.setOperatingCostPerUnitVnd(BigDecimal.ZERO);
        dssProperties.setDefaultForecastDays(30);
        DssProfitCalculator profitCalculator = new DssProfitCalculator(dssProperties);
        DssScenarioEngine scenarioEngine = new DssScenarioEngine(dssProperties, profitCalculator);
        pricePredictionService = new PricePredictionServiceImpl(
            productService,
            orderService,
            dssProperties,
            scenarioEngine,
            productContextService,
            predictionInsightService
        );
        lenient().when(productContextService.buildContext(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(DssProductContextResponse.builder()
            .performanceSummary("Test context")
            .build());
        lenient().when(predictionInsightService.generatePriceInsight(org.mockito.ArgumentMatchers.any()))
            .thenReturn(DssAiInsightResponse.builder()
                .title("Giá")
                .summary("Test insight")
                .fallback(true)
                .build());
    }

    @Test
    void generatePredictionBuildsFiveScenariosAndSelectsBestProfit() {
        GeneratePricePredictionRequest request = buildRequest();

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(buildProductInfo(SELLER_ID));
        when(productService.getPriceHistoryInfo(
            PRODUCT_ID,
            FROM_DATE,
            TO_DATE
        )).thenReturn(List.of(
            new PriceHistoryInfo(
                new BigDecimal("80.00"),
                new BigDecimal("100.00"),
                OffsetDateTime.of(2026, 7, 16, 10, 0, 0, 0, ZoneOffset.UTC)
            )
        ));
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 15)
        )).thenReturn(160L);
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            LocalDate.of(2026, 7, 16),
            LocalDate.of(2026, 7, 30)
        )).thenReturn(40L);
        when(orderService.getFirstCompletedSaleDate(PRODUCT_ID))
            .thenReturn(LocalDate.of(2026, 6, 1));
        when(orderService.getCompletedDailySalesMap(
            PRODUCT_ID,
            FROM_DATE,
            TO_DATE
        )).thenReturn(java.util.Map.of(
            LocalDate.of(2026, 7, 10), 80L,
            LocalDate.of(2026, 7, 20), 40L
        ));

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId)
                .thenReturn(SELLER_ID);

            PricePredictionResponse response =
                pricePredictionService.generatePrediction(request);

            assertEquals(PRODUCT_ID, response.getProductId());
            assertEquals(new BigDecimal("100.00"), response.getCurrentPrice());
            assertEquals(new BigDecimal("70.00"), response.getCost());
            assertEquals(new BigDecimal("-3.0000"), response.getAverageElasticity());
            assertEquals(200L, response.getTotalQuantitySold());
            assertEquals(5, response.getScenarios().size());

            assertScenario(
                response.getScenarios().get(0),
                -10,
                "90.00",
                "20.00",
                260L,
                "5200.00"
            );
            assertScenario(
                response.getScenarios().get(1),
                -5,
                "95.00",
                "25.00",
                230L,
                "5750.00"
            );
            assertScenario(
                response.getScenarios().get(2),
                0,
                "100.00",
                "30.00",
                200L,
                "6000.00"
            );
            assertScenario(
                response.getScenarios().get(3),
                5,
                "105.00",
                "35.00",
                170L,
                "5950.00"
            );
            assertScenario(
                response.getScenarios().get(4),
                10,
                "110.00",
                "40.00",
                140L,
                "5600.00"
            );

            assertEquals(
                0,
                response.getBestScenario().getPriceChangePercent()
            );
            assertEquals(
                new BigDecimal("6000.00"),
                response.getBestScenario().getExpectedProfit()
            );
        }
    }

    @Test
    void generatePredictionForcesPositiveRawElasticityToNegative() {
        GeneratePricePredictionRequest request = buildRequest();

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(buildProductInfo(SELLER_ID));
        when(productService.getPriceHistoryInfo(
            PRODUCT_ID,
            FROM_DATE,
            TO_DATE
        )).thenReturn(List.of(
            new PriceHistoryInfo(
                new BigDecimal("80.00"),
                new BigDecimal("100.00"),
                OffsetDateTime.of(2026, 7, 16, 10, 0, 0, 0, ZoneOffset.UTC)
            )
        ));
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 15)
        )).thenReturn(80L);
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            LocalDate.of(2026, 7, 16),
            LocalDate.of(2026, 7, 30)
        )).thenReturn(120L);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId)
                .thenReturn(SELLER_ID);

            PricePredictionResponse response =
                pricePredictionService.generatePrediction(request);

            assertEquals(new BigDecimal("-2.0000"), response.getAverageElasticity());
        }
    }

    @Test
    void generatePredictionRejectsInvalidPrice() {
        GeneratePricePredictionRequest request = buildRequest();
        ProductInfo product = new ProductInfo(
            PRODUCT_ID,
            SELLER_ID,
            "Wireless Mouse",
            BigDecimal.ZERO,
            null,
            ProductStatus.ACTIVE
        );

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(product);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId)
                .thenReturn(SELLER_ID);

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pricePredictionService.generatePrediction(request)
            );

            assertEquals(
                "Giá bán sản phẩm phải lớn hơn 0.",
                exception.getMessage()
            );
        }

        verify(productService, never())
            .getPriceHistoryInfo(PRODUCT_ID, FROM_DATE, TO_DATE);
    }

    @Test
    void generatePredictionRejectsProductOwnedByAnotherSeller() {
        GeneratePricePredictionRequest request = buildRequest();

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(buildProductInfo(99L));

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId)
                .thenReturn(SELLER_ID);

            assertThrows(
                ForbiddenException.class,
                () -> pricePredictionService.generatePrediction(request)
            );
        }
    }

    @Test
    void generatePredictionRejectsRangeWithoutPriceChange() {
        GeneratePricePredictionRequest request = buildRequest();

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(buildProductInfo(SELLER_ID));
        when(productService.getPriceHistoryInfo(
            PRODUCT_ID,
            FROM_DATE,
            TO_DATE
        )).thenReturn(List.of());

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId)
                .thenReturn(SELLER_ID);

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pricePredictionService.generatePrediction(request)
            );

            assertEquals(
                "Không đủ dữ liệu để tạo khuyến nghị giá.",
                exception.getMessage()
            );
        }
    }

    @Test
    void calculateElasticityUsesCompletedSalesAcrossPriceRegimes() {
        LocalDate firstSaleDate = LocalDate.now().minusDays(30);
        LocalDate changeDate = LocalDate.now().minusDays(15);

        when(orderService.getFirstCompletedSaleDate(PRODUCT_ID))
            .thenReturn(firstSaleDate);
        when(productService.getPriceHistory(PRODUCT_ID))
            .thenReturn(List.of(
                PriceHistoryResponse.builder()
                    .oldPrice(new BigDecimal("80.00"))
                    .newPrice(new BigDecimal("100.00"))
                    .changedAt(changeDate.atTime(10, 0).atOffset(ZoneOffset.UTC))
                    .build()
            ));
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            firstSaleDate,
            changeDate.minusDays(1)
        )).thenReturn(160L);
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            changeDate,
            LocalDate.now()
        )).thenReturn(40L);

        double elasticity = pricePredictionService
            .calculateElasticity(PRODUCT_ID);

        assertTrue(elasticity < 0);
    }

    private GeneratePricePredictionRequest buildRequest() {
        GeneratePricePredictionRequest request =
            new GeneratePricePredictionRequest();
        request.setProductId(PRODUCT_ID);
        request.setFromDate(FROM_DATE);
        request.setToDate(TO_DATE);
        return request;
    }

    private ProductInfo buildProductInfo(Long sellerId) {
        return new ProductInfo(
            PRODUCT_ID,
            sellerId,
            "Wireless Mouse",
            new BigDecimal("100.00"),
            new BigDecimal("70.00"),
            ProductStatus.ACTIVE
        );
    }

    private void assertScenario(
        PriceScenarioResponse scenario,
        int priceChangePercent,
        String newPrice,
        String profitPerProduct,
        long predictedDemand,
        String expectedProfit
    ) {
        assertEquals(priceChangePercent, scenario.getPriceChangePercent());
        assertEquals(new BigDecimal("70.00"), scenario.getCost());
        assertEquals(new BigDecimal(newPrice), scenario.getNewPrice());
        assertEquals(
            new BigDecimal(profitPerProduct),
            scenario.getProfitPerProduct()
        );
        assertEquals(predictedDemand, scenario.getPredictedDemand());
        assertEquals(
            new BigDecimal(expectedProfit),
            scenario.getExpectedProfit()
        );
    }
}

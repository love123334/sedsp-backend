package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.util.AppTime;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastComputation;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastProductView;
import com.example.secdsp.modules.dss.dto.internal.PriceElasticitySnapshot;
import com.example.secdsp.modules.dss.dto.request.CreateAdvancedPriceScenarioRequest;
import com.example.secdsp.modules.dss.dto.request.CreateAdvancedPriceSessionRequest;
import com.example.secdsp.modules.dss.dto.response.AdvancedPriceSessionResponse;
import com.example.secdsp.modules.dss.dto.response.ApplyAdvancedPriceScenarioResponse;
import com.example.secdsp.modules.dss.entity.AdvancedPriceScenario;
import com.example.secdsp.modules.dss.entity.AdvancedPriceSession;
import com.example.secdsp.modules.dss.entity.AdvancedPriceSessionStatus;
import com.example.secdsp.modules.dss.repository.AdvancedPriceScenarioRepository;
import com.example.secdsp.modules.dss.repository.AdvancedPriceSessionRepository;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.dto.request.UpdateProductRequest;
import com.example.secdsp.modules.product.entity.ProductStatus;
import com.example.secdsp.modules.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvancedPriceAnalysisServiceImplTest {

    private static final Long SELLER_ID = 7L;
    private static final Long PRODUCT_ID = 15L;
    private static final Long SESSION_ID = 21L;

    @Mock
    AdvancedPriceSessionRepository sessionRepository;

    @Mock
    AdvancedPriceScenarioRepository scenarioRepository;

    @Mock
    ProductService productService;

    @Mock
    OrderService orderService;

    @Mock
    PriceElasticityService priceElasticityService;

    @Mock
    DemandForecastEngine demandForecastEngine;

    @InjectMocks
    AdvancedPriceAnalysisServiceImpl service;

    @Test
    void createSessionReusesElasticityAndDemandForecastEngine() {
        CreateAdvancedPriceSessionRequest request = sessionRequest();
        ProductInfo product = product("100.00");

        when(productService.getProductInfo(PRODUCT_ID)).thenReturn(product);
        when(priceElasticityService.analyze(
            PRODUCT_ID,
            request.getFromDate(),
            request.getToDate()
        )).thenReturn(new PriceElasticitySnapshot(
            new BigDecimal("-2.0000"),
            200L
        ));
        when(demandForecastEngine.forecast(
            any(DemandForecastProductView.class),
            any(LocalDate.class),
            any(LocalDate.class),
            any(Integer.class)
        )).thenReturn(forecast(70L));
        when(sessionRepository.save(any(AdvancedPriceSession.class)))
            .thenAnswer(invocation -> {
                AdvancedPriceSession session = invocation.getArgument(0);
                session.setId(SESSION_ID);
                session.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                return session;
            });

        try (MockedStatic<SecurityUtils> security = mockSeller()) {
            AdvancedPriceSessionResponse response = service.createSession(request);

            assertEquals(SESSION_ID, response.getSessionId());
            assertEquals("SELECTED_RANGE", response.getElasticitySource());
            assertEquals(new BigDecimal("-2.0000"), response.getAverageElasticity());
            assertEquals(70L, response.getBaselineForecastDemand());
            assertEquals("lightgbm_onnx", response.getForecastMethod());
            assertEquals(200L, response.getProductSummary().getHistoricalQuantitySold());
            assertEquals(new BigDecimal("5.00"), response.getProductSummary().getEstimatedOrderCost());
        }

        verify(demandForecastEngine).forecast(
            any(DemandForecastProductView.class),
            any(LocalDate.class),
            any(LocalDate.class),
            any(Integer.class)
        );
    }

    @Test
    void createSessionFallsBackToAllPriceHistoryForElasticity() {
        CreateAdvancedPriceSessionRequest request = sessionRequest();
        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(product("100.00"));
        when(priceElasticityService.analyze(
            PRODUCT_ID,
            request.getFromDate(),
            request.getToDate()
        ))
            .thenThrow(new BusinessException("Selected range has one price."));
        when(priceElasticityService.analyzeAllHistory(PRODUCT_ID))
            .thenReturn(new PriceElasticitySnapshot(
                new BigDecimal("-1.5000"),
                120L
            ));
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            request.getFromDate(),
            request.getToDate()
        )).thenReturn(80L);
        when(demandForecastEngine.forecast(
            any(DemandForecastProductView.class),
            any(LocalDate.class),
            any(LocalDate.class),
            any(Integer.class)
        )).thenReturn(forecast(70L));
        when(sessionRepository.save(any(AdvancedPriceSession.class)))
            .thenAnswer(invocation -> {
                AdvancedPriceSession session = invocation.getArgument(0);
                session.setId(SESSION_ID);
                session.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                return session;
            });

        try (MockedStatic<SecurityUtils> security = mockSeller()) {
            AdvancedPriceSessionResponse response = service.createSession(request);

            assertEquals("ALL_HISTORY_FALLBACK", response.getElasticitySource());
            assertEquals(new BigDecimal("-1.500000"), response.getAverageElasticity());
            assertEquals(80L, response.getProductSummary().getHistoricalQuantitySold());
        }
    }

    @Test
    void createScenarioCombinesLightGbmDemandWithElasticityAndCosts() {
        AdvancedPriceSession session = activeSession();
        List<AdvancedPriceScenario> stored = prepareScenarioStorage();
        when(sessionRepository.findOwnedByIdForUpdate(SESSION_ID, SELLER_ID))
            .thenReturn(Optional.of(session));
        when(scenarioRepository.existsBySessionIdAndPriceChangePercent(
            SESSION_ID,
            new BigDecimal("-10.00")
        )).thenReturn(false);

        CreateAdvancedPriceScenarioRequest request = scenarioRequest("-10");

        try (MockedStatic<SecurityUtils> security = mockSeller()) {
            AdvancedPriceSessionResponse response = service.createScenario(
                SESSION_ID,
                request
            );

            assertEquals(1, stored.size());
            assertEquals(new BigDecimal("90.00"), response.getLatestScenario().getNewPrice());
            assertEquals(new BigDecimal("15.00"), response.getLatestScenario().getProfitPerProduct());
            assertEquals(new BigDecimal("1.200000"), response.getLatestScenario().getDemandMultiplier());
            assertEquals(120L, response.getLatestScenario().getForecastDemand());
            assertEquals(new BigDecimal("1800.00"), response.getLatestScenario().getExpectedProfit());
        }
    }

    @Test
    void createScenarioKeepsOnlyFiveMostRecentRows() {
        AdvancedPriceSession session = activeSession();
        List<AdvancedPriceScenario> stored = prepareScenarioStorage();
        for (int index = 0; index < 5; index++) {
            stored.add(scenario(
                (long) (index + 1),
                BigDecimal.valueOf(index),
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(index)
            ));
        }
        when(sessionRepository.findOwnedByIdForUpdate(SESSION_ID, SELLER_ID))
            .thenReturn(Optional.of(session));
        when(scenarioRepository.existsBySessionIdAndPriceChangePercent(
            SESSION_ID,
            new BigDecimal("10.00")
        )).thenReturn(false);
        org.mockito.Mockito.doAnswer(invocation -> {
            stored.remove(invocation.getArgument(0));
            return null;
        }).when(scenarioRepository).delete(any(AdvancedPriceScenario.class));

        try (MockedStatic<SecurityUtils> security = mockSeller()) {
            AdvancedPriceSessionResponse response = service.createScenario(
                SESSION_ID,
                scenarioRequest("10")
            );

            assertEquals(5, response.getScenarioCount());
            assertEquals(5, stored.size());
            verify(scenarioRepository).delete(any(AdvancedPriceScenario.class));
        }
    }

    @Test
    void createScenarioRejectsPercentageOutsideSliderRange() {
        when(sessionRepository.findOwnedByIdForUpdate(SESSION_ID, SELLER_ID))
            .thenReturn(Optional.of(activeSession()));

        try (MockedStatic<SecurityUtils> security = mockSeller()) {
            assertThrows(
                BusinessException.class,
                () -> service.createScenario(
                    SESSION_ID,
                    scenarioRequest("100.01")
                )
            );
        }

        verify(scenarioRepository, never()).save(any());
    }

    @Test
    void applyScenarioUpdatesProductAndLocksSession() {
        AdvancedPriceSession session = activeSession();
        AdvancedPriceScenario scenario = scenario(
            31L,
            new BigDecimal("-10.00"),
            OffsetDateTime.now(ZoneOffset.UTC)
        );
        scenario.setNewPrice(new BigDecimal("90.00"));

        when(sessionRepository.findOwnedByIdForUpdate(SESSION_ID, SELLER_ID))
            .thenReturn(Optional.of(session));
        when(scenarioRepository.findByIdAndSessionId(31L, SESSION_ID))
            .thenReturn(Optional.of(scenario));
        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(product("100.00"));

        try (MockedStatic<SecurityUtils> security = mockSeller()) {
            ApplyAdvancedPriceScenarioResponse response = service.applyScenario(
                SESSION_ID,
                31L
            );

            assertEquals(new BigDecimal("100.00"), response.getOldPrice());
            assertEquals(new BigDecimal("90.00"), response.getNewPrice());
            assertEquals(AdvancedPriceSessionStatus.APPLIED, session.getStatus());
            assertNotNull(session.getAppliedAt());
        }

        ArgumentCaptor<UpdateProductRequest> requestCaptor =
            ArgumentCaptor.forClass(UpdateProductRequest.class);
        verify(productService).updateProduct(
            org.mockito.ArgumentMatchers.eq(PRODUCT_ID),
            requestCaptor.capture()
        );
        assertEquals(new BigDecimal("90.00"), requestCaptor.getValue().getPrice());
    }

    @Test
    void applyScenarioRejectsStaleBasePrice() {
        AdvancedPriceSession session = activeSession();
        AdvancedPriceScenario scenario = scenario(
            31L,
            new BigDecimal("-10.00"),
            OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(sessionRepository.findOwnedByIdForUpdate(SESSION_ID, SELLER_ID))
            .thenReturn(Optional.of(session));
        when(scenarioRepository.findByIdAndSessionId(31L, SESSION_ID))
            .thenReturn(Optional.of(scenario));
        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(product("105.00"));

        try (MockedStatic<SecurityUtils> security = mockSeller()) {
            assertThrows(
                BusinessException.class,
                () -> service.applyScenario(SESSION_ID, 31L)
            );
        }

        verify(productService, never()).updateProduct(any(), any());
    }

    private List<AdvancedPriceScenario> prepareScenarioStorage() {
        List<AdvancedPriceScenario> stored = new ArrayList<>();
        when(scenarioRepository.findBySessionIdOrderByCreatedAtDesc(SESSION_ID))
            .thenAnswer(invocation -> List.copyOf(stored));
        when(scenarioRepository.save(any(AdvancedPriceScenario.class)))
            .thenAnswer(invocation -> {
                AdvancedPriceScenario scenario = invocation.getArgument(0);
                if (scenario.getId() == null) {
                    scenario.setId(100L + stored.size());
                    scenario.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    stored.add(0, scenario);
                }
                return scenario;
            });
        return stored;
    }

    private CreateAdvancedPriceSessionRequest sessionRequest() {
        CreateAdvancedPriceSessionRequest request =
            new CreateAdvancedPriceSessionRequest();
        request.setProductId(PRODUCT_ID);
        request.setFromDate(AppTime.today().minusDays(29));
        request.setToDate(AppTime.today());
        request.setForecastPeriod(7);
        request.setEstimatedOrderCost(new BigDecimal("5.00"));
        return request;
    }

    private CreateAdvancedPriceScenarioRequest scenarioRequest(String change) {
        CreateAdvancedPriceScenarioRequest request =
            new CreateAdvancedPriceScenarioRequest();
        request.setPriceChangePercent(new BigDecimal(change));
        return request;
    }

    private AdvancedPriceSession activeSession() {
        return AdvancedPriceSession.builder()
            .id(SESSION_ID)
            .sellerId(SELLER_ID)
            .productId(PRODUCT_ID)
            .productName("Demo Product")
            .fromDate(AppTime.today().minusDays(29))
            .toDate(AppTime.today())
            .forecastPeriod(7)
            .estimatedOrderCost(new BigDecimal("5.00"))
            .basePrice(new BigDecimal("100.00"))
            .costPrice(new BigDecimal("70.00"))
            .historicalQuantitySold(200L)
            .averageElasticity(new BigDecimal("-2.000000"))
            .elasticitySource("SELECTED_RANGE")
            .baselineForecastDemand(100L)
            .forecastMethod("lightgbm_onnx")
            .status(AdvancedPriceSessionStatus.ACTIVE)
            .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
            .build();
    }

    private AdvancedPriceScenario scenario(
        Long id,
        BigDecimal change,
        OffsetDateTime createdAt
    ) {
        return AdvancedPriceScenario.builder()
            .id(id)
            .sessionId(SESSION_ID)
            .priceChangePercent(change)
            .newPrice(new BigDecimal("100.00"))
            .profitPerProduct(new BigDecimal("25.00"))
            .demandMultiplier(BigDecimal.ONE)
            .forecastDemand(100L)
            .expectedProfit(new BigDecimal("2500.00"))
            .createdAt(createdAt)
            .build();
    }

    private ProductInfo product(String price) {
        return new ProductInfo(
            PRODUCT_ID,
            SELLER_ID,
            "Demo Product",
            new BigDecimal(price),
            new BigDecimal("70.00"),
            ProductStatus.ACTIVE
        );
    }

    private DemandForecastComputation forecast(long demand) {
        return new DemandForecastComputation(
            PRODUCT_ID,
            "Demo Product",
            30,
            7,
            10.0,
            demand,
            "lightgbm_onnx",
            false,
            List.of(),
            List.of(),
            Map.of(),
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
    }

    private MockedStatic<SecurityUtils> mockSeller() {
        MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getCurrentUserId).thenReturn(SELLER_ID);
        return security;
    }
}

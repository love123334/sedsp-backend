package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.request.GenerateDemandPredictionRequest;
import com.example.secdsp.modules.dss.dto.response.DemandPredictionResponse;
import com.example.secdsp.modules.dss.entity.DemandPrediction;
import com.example.secdsp.modules.dss.mapper.DemandPredictionMapper;
import com.example.secdsp.modules.dss.repository.DemandPredictionRepository;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.ProductStatus;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandPredictionServiceImplTest {

    private static final Long PRODUCT_ID = 15L;
    private static final Long SELLER_ID = 7L;

    @Mock
    DemandPredictionRepository demandPredictionRepository;

    @Mock
    DemandPredictionMapper demandPredictionMapper;

    @Mock
    ProductService productService;

    @Mock
    OrderService orderService;

    @InjectMocks
    DemandPredictionServiceImpl demandPredictionService;

    @Test
    void generatePredictionCalculatesSmaAndSavesResult() {
        GenerateDemandPredictionRequest request = buildRequest(90, 30);
        ProductInfo product = buildProductInfo();
        DemandPredictionResponse expectedResponse =
            DemandPredictionResponse.builder()
                .productName(product.name())
                .historicalDays(90)
                .forecastPeriod(30)
                .averageDailyDemand(new BigDecimal("10.00"))
                .predictedDemand(new BigDecimal("300.00"))
                .build();

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(product);
        when(orderService.getFirstCompletedSaleDate(PRODUCT_ID))
            .thenReturn(LocalDate.now().minusDays(120));
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            LocalDate.now().minusDays(89),
            LocalDate.now()
        )).thenReturn(900L);
        when(demandPredictionRepository.save(any(DemandPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(demandPredictionMapper.toResponse(any(DemandPrediction.class)))
            .thenReturn(expectedResponse);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId)
                .thenReturn(SELLER_ID);
            securityUtils.when(() -> SecurityUtils.hasRole(UserRole.SELLER))
                .thenReturn(true);

            DemandPredictionResponse actual =
                demandPredictionService.generatePrediction(request);

            assertEquals(expectedResponse, actual);
        }

        ArgumentCaptor<DemandPrediction> predictionCaptor =
            ArgumentCaptor.forClass(DemandPrediction.class);
        verify(demandPredictionRepository).save(predictionCaptor.capture());

        DemandPrediction saved = predictionCaptor.getValue();
        assertEquals(new BigDecimal("10.00"), saved.getAverageDailyDemand());
        assertEquals(new BigDecimal("300.00"), saved.getPredictedQuantity());
        assertEquals(PRODUCT_ID, saved.getProduct().getId());
        assertEquals(SELLER_ID, saved.getGeneratedBy().getId());
    }

    @Test
    void generatePredictionPropagatesProductNotFound() {
        GenerateDemandPredictionRequest request = buildRequest(90, 30);

        when(productService.getProductInfo(PRODUCT_ID))
            .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

        assertThrows(
            ResourceNotFoundException.class,
            () -> demandPredictionService.generatePrediction(request)
        );

        verify(orderService, never())
            .getFirstCompletedSaleDate(any());
        verify(demandPredictionRepository, never())
            .save(any());
    }

    @Test
    void generatePredictionRejectsProductWithoutSalesHistory() {
        GenerateDemandPredictionRequest request = buildRequest(90, 30);

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(buildProductInfo());
        when(orderService.getFirstCompletedSaleDate(PRODUCT_ID))
            .thenReturn(null);

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockSellerSecurityContext()) {
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> demandPredictionService.generatePrediction(request)
            );

            assertEquals(
                "Không đủ dữ liệu để tạo dự báo.",
                exception.getMessage()
            );
        }

        verify(demandPredictionRepository, never())
            .save(any());
    }

    @Test
    void generatePredictionRejectsInsufficientHistoricalDays() {
        GenerateDemandPredictionRequest request = buildRequest(90, 30);

        when(productService.getProductInfo(PRODUCT_ID))
            .thenReturn(buildProductInfo());
        when(orderService.getFirstCompletedSaleDate(PRODUCT_ID))
            .thenReturn(LocalDate.now().minusDays(44));

        try (MockedStatic<SecurityUtils> securityUtils =
                 mockSellerSecurityContext()) {
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> demandPredictionService.generatePrediction(request)
            );

            assertEquals(
                "Không đủ dữ liệu để tạo dự báo.",
                exception.getMessage()
            );
        }

        verify(orderService, never())
            .getCompletedQuantitySold(any(), any(), any());
        verify(demandPredictionRepository, never())
            .save(any());
    }

    private MockedStatic<SecurityUtils> mockSellerSecurityContext() {
        MockedStatic<SecurityUtils> securityUtils =
            mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId)
            .thenReturn(SELLER_ID);
        securityUtils.when(() -> SecurityUtils.hasRole(UserRole.SELLER))
            .thenReturn(true);
        return securityUtils;
    }

    private GenerateDemandPredictionRequest buildRequest(
        int historicalDays,
        int forecastPeriod
    ) {
        GenerateDemandPredictionRequest request =
            new GenerateDemandPredictionRequest();
        request.setProductId(PRODUCT_ID);
        request.setHistoricalDays(historicalDays);
        request.setForecastPeriod(forecastPeriod);
        return request;
    }

    private ProductInfo buildProductInfo() {
        return new ProductInfo(
            PRODUCT_ID,
            SELLER_ID,
            "Nike Air Force",
            new BigDecimal("2500000.00"),
            ProductStatus.ACTIVE
        );
    }
}

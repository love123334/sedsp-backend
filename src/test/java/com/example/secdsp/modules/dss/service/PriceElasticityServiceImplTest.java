package com.example.secdsp.modules.dss.service;

import com.example.secdsp.modules.dss.dto.internal.PriceElasticitySnapshot;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.PriceHistoryInfo;
import com.example.secdsp.modules.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceElasticityServiceImplTest {

    private static final long PRODUCT_ID = 15L;

    @Mock
    ProductService productService;

    @Mock
    OrderService orderService;

    @InjectMocks
    PriceElasticityServiceImpl service;

    @Test
    void calculatesElasticityAndQuantityAcrossPriceRegimes() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 10);
        OffsetDateTime changedAt = OffsetDateTime.of(
            2026,
            1,
            6,
            0,
            0,
            0,
            0,
            ZoneOffset.UTC
        );

        when(productService.getPriceHistoryInfo(PRODUCT_ID, fromDate, toDate))
            .thenReturn(List.of(new PriceHistoryInfo(
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                changedAt
            )));
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            fromDate,
            LocalDate.of(2026, 1, 5)
        )).thenReturn(50L);
        when(orderService.getCompletedQuantitySold(
            PRODUCT_ID,
            LocalDate.of(2026, 1, 6),
            toDate
        )).thenReturn(75L);

        PriceElasticitySnapshot result = service.analyze(
            PRODUCT_ID,
            fromDate,
            toDate
        );

        assertEquals(new BigDecimal("-5.0000"), result.averageElasticity());
        assertEquals(125L, result.quantitySold());
    }
}

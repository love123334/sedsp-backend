package com.example.secdsp.modules.platformrevenue.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.order.entity.OrderStatus;
import com.example.secdsp.modules.payment.entity.PaymentMethod;
import com.example.secdsp.modules.platformrevenue.dto.request.PlatformRevenueDashboardRequest;
import com.example.secdsp.modules.platformrevenue.dto.request.RevenueGranularity;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse;
import com.example.secdsp.modules.platformrevenue.repository.PlatformRevenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformRevenueServiceImplTest {

    private static final LocalDate FROM_DATE =
        LocalDate.of(2026, 7, 1);
    private static final LocalDate TO_DATE =
        LocalDate.of(2026, 7, 3);
    private static final LocalDateTime START_DATE_TIME =
        FROM_DATE.atStartOfDay();
    private static final LocalDateTime END_DATE_TIME =
        TO_DATE.plusDays(1).atStartOfDay();
    private static final LocalDateTime PREVIOUS_START_DATE_TIME =
        FROM_DATE.minusDays(3).atStartOfDay();

    @Mock
    PlatformRevenueRepository platformRevenueRepository;

    @InjectMocks
    PlatformRevenueServiceImpl platformRevenueService;

    @Test
    void getDashboardBuildsDailyReportAndFillsMissingPeriods() {
        PlatformRevenueDashboardRequest request = buildRequest(
            FROM_DATE,
            TO_DATE,
            RevenueGranularity.DAY
        );

        when(platformRevenueRepository.findOrderOverview(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(List.<Object[]>of(new Object[] {
            new BigDecimal("1000.00"),
            new BigDecimal("1050.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            10L,
            4L,
            3L
        }));
        when(platformRevenueRepository.findItemOverview(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(List.<Object[]>of(new Object[] {20L, 2L}));
        when(platformRevenueRepository.findGrossMerchandiseValue(
            PREVIOUS_START_DATE_TIME,
            START_DATE_TIME
        )).thenReturn(new BigDecimal("800.00"));
        when(platformRevenueRepository.findSuccessfulPaymentAmount(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(new BigDecimal("900.00"));
        when(platformRevenueRepository.findOrderStatusDistribution(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(List.of(
            new Object[] {"PENDING", 4L},
            new Object[] {"DELIVERED", 4L},
            new Object[] {"CANCELLED", 2L}
        ));
        when(platformRevenueRepository.findDailyRevenueTrend(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(List.of(
            new Object[] {
                FROM_DATE,
                new BigDecimal("400.00"),
                new BigDecimal("420.00"),
                2L,
                8L
            },
            new Object[] {
                TO_DATE,
                new BigDecimal("600.00"),
                new BigDecimal("630.00"),
                2L,
                12L
            }
        ));
        when(platformRevenueRepository.findTopSellers(
            START_DATE_TIME,
            END_DATE_TIME,
            5
        )).thenReturn(List.<Object[]>of(new Object[] {
            7L,
            "Alpha Store",
            new BigDecimal("600.00"),
            2L,
            12L
        }));
        when(platformRevenueRepository.findTopProducts(
            START_DATE_TIME,
            END_DATE_TIME,
            5
        )).thenReturn(List.<Object[]>of(new Object[] {
            15L,
            "Wireless Mouse",
            7L,
            "Alpha Store",
            new BigDecimal("500.00"),
            2L,
            10L
        }));
        when(platformRevenueRepository.findTopCategories(
            START_DATE_TIME,
            END_DATE_TIME,
            5
        )).thenReturn(List.<Object[]>of(new Object[] {
            3L,
            "Electronics",
            new BigDecimal("700.00"),
            3L,
            14L
        }));
        when(platformRevenueRepository.findPaymentMethodDistribution(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(List.of(
            new Object[] {
                "MOMO",
                5L,
                3L,
                1L,
                1L,
                new BigDecimal("600.00")
            },
            new Object[] {
                "VNPAY",
                2L,
                1L,
                1L,
                0L,
                new BigDecimal("300.00")
            }
        ));
        when(platformRevenueRepository.findUserActivity(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(List.<Object[]>of(new Object[] {5L, 4L, 1L, 100L, 90L, 10L}));
        when(platformRevenueRepository.findProductActivity(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(List.<Object[]>of(new Object[] {50L, 40L, 5L, 5L, 8L, 2L}));
        when(platformRevenueRepository.countActiveCategories())
            .thenReturn(7L);
        when(platformRevenueRepository.findDailyActivityTrend(
            START_DATE_TIME,
            END_DATE_TIME
        )).thenReturn(List.<Object[]>of(new Object[] {
            FROM_DATE.plusDays(1),
            1L,
            2L,
            3L
        }));

        PlatformRevenueDashboardResponse response =
            platformRevenueService.getDashboard(request);

        assertEquals(FROM_DATE, response.period().fromDate());
        assertEquals(TO_DATE, response.period().toDate());
        assertEquals(RevenueGranularity.DAY, response.period().granularity());

        assertEquals(
            new BigDecimal("1000.00"),
            response.overview().grossMerchandiseValue()
        );
        assertEquals(
            new BigDecimal("800.00"),
            response.overview().previousPeriodGmv()
        );
        assertEquals(
            new BigDecimal("25.00"),
            response.overview().gmvGrowthPercentage()
        );
        assertEquals(
            new BigDecimal("262.50"),
            response.overview().averageOrderValue()
        );
        assertEquals(4L, response.overview().deliveredOrders());
        assertEquals(20L, response.overview().unitsSold());

        PlatformRevenueDashboardResponse.OrderStatusDistributionItem delivered =
            response.orderStatusDistribution().stream()
                .filter(item -> item.status() == OrderStatus.DELIVERED)
                .findFirst()
                .orElseThrow();
        assertEquals(4L, delivered.orderCount());
        assertEquals(new BigDecimal("40.00"), delivered.percentage());
        assertEquals(OrderStatus.values().length,
                     response.orderStatusDistribution().size());

        assertEquals(3, response.revenueTrend().size());
        assertEquals(FROM_DATE.plusDays(1),
                     response.revenueTrend().get(1).periodStart());
        assertEquals(new BigDecimal("0.00"),
                     response.revenueTrend().get(1).grossMerchandiseValue());
        assertEquals(0L, response.revenueTrend().get(1).deliveredOrders());
        assertEquals(0L, response.revenueTrend().get(1).unitsSold());
        assertEquals(new BigDecimal("420.00"),
                     response.revenueTrend().get(0).deliveredOrderValue());

        assertEquals(7L, response.topSellers().get(0).sellerId());
        assertEquals("Alpha Store", response.topSellers().get(0).sellerName());
        assertEquals(new BigDecimal("60.00"),
                     response.topSellers().get(0).marketSharePercentage());
        assertEquals(15L, response.topProducts().get(0).productId());
        assertEquals(new BigDecimal("500.00"),
                     response.topProducts().get(0).grossMerchandiseValue());
        assertEquals(10L, response.topProducts().get(0).unitsSold());
        assertEquals(2L, response.topProducts().get(0).deliveredOrders());
        assertEquals(3L, response.topCategories().get(0).categoryId());
        assertEquals(3L, response.topCategories().get(0).deliveredOrders());
        assertEquals(14L, response.topCategories().get(0).unitsSold());
        assertEquals(new BigDecimal("70.00"),
                     response.topCategories().get(0).marketSharePercentage());

        PlatformRevenueDashboardResponse.PaymentMethodDistributionItem momo =
            response.paymentMethodDistribution().stream()
                .filter(item -> item.paymentMethod() == PaymentMethod.MOMO)
                .findFirst()
                .orElseThrow();
        assertEquals(3L, momo.successfulPaymentCount());
        assertEquals(5L, momo.totalPaymentCount());
        assertEquals(1L, momo.pendingPaymentCount());
        assertEquals(1L, momo.failedPaymentCount());
        assertEquals(new BigDecimal("66.67"), momo.percentage());
        assertEquals(PaymentMethod.values().length,
                     response.paymentMethodDistribution().size());

        assertEquals(5L, response.platformActivity().totalSellers());
        assertEquals(90L,
                     response.platformActivity().activeCustomerAccounts());
        assertEquals(7L, response.platformActivity().totalCategories());
        assertEquals(3, response.activityTrend().size());
        assertEquals(0L, response.activityTrend().get(0).newSellers());
        assertEquals(1L, response.activityTrend().get(1).newSellers());
        assertEquals(2L, response.activityTrend().get(1).newCustomers());
        assertEquals(3L, response.activityTrend().get(1).newProducts());
        assertEquals(0L, response.activityTrend().get(2).newProducts());
    }

    @Test
    void getDashboardFillsMissingMonthlyPeriods() {
        LocalDate fromDate = LocalDate.of(2026, 1, 15);
        LocalDate toDate = LocalDate.of(2026, 3, 10);
        LocalDateTime startDateTime = fromDate.atStartOfDay();
        LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay();
        PlatformRevenueDashboardRequest request = buildRequest(
            fromDate,
            toDate,
            RevenueGranularity.MONTH
        );

        when(platformRevenueRepository.findMonthlyRevenueTrend(
            startDateTime,
            endDateTime
        )).thenReturn(List.of(
            new Object[] {
                LocalDate.of(2026, 1, 1),
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                1L,
                2L
            },
            new Object[] {
                LocalDate.of(2026, 3, 1),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                3L,
                6L
            }
        ));
        when(platformRevenueRepository.findMonthlyActivityTrend(
            startDateTime,
            endDateTime
        )).thenReturn(List.<Object[]>of(new Object[] {
            LocalDate.of(2026, 3, 1),
            2L,
            4L,
            6L
        }));

        PlatformRevenueDashboardResponse response =
            platformRevenueService.getDashboard(request);

        assertEquals(3, response.revenueTrend().size());
        assertEquals(LocalDate.of(2026, 1, 1),
                     response.revenueTrend().get(0).periodStart());
        assertEquals(LocalDate.of(2026, 2, 1),
                     response.revenueTrend().get(1).periodStart());
        assertEquals(new BigDecimal("0.00"),
                     response.revenueTrend().get(1).grossMerchandiseValue());
        assertEquals(LocalDate.of(2026, 3, 1),
                     response.revenueTrend().get(2).periodStart());
        assertEquals(new BigDecimal("300.00"),
                     response.revenueTrend().get(2).grossMerchandiseValue());

        assertEquals(3, response.activityTrend().size());
        assertEquals(LocalDate.of(2026, 2, 1),
                     response.activityTrend().get(1).periodStart());
        assertEquals(0L, response.activityTrend().get(1).newCustomers());
        assertEquals(2L, response.activityTrend().get(2).newSellers());
        assertEquals(6L, response.activityTrend().get(2).newProducts());
        assertNull(response.overview().gmvGrowthPercentage());
    }

    @Test
    void getDashboardRejectsReversedDateRange() {
        PlatformRevenueDashboardRequest request = buildRequest(
            LocalDate.of(2026, 7, 10),
            LocalDate.of(2026, 7, 1),
            RevenueGranularity.DAY
        );

        assertThrows(
            BusinessException.class,
            () -> platformRevenueService.getDashboard(request)
        );
        verifyNoInteractions(platformRevenueRepository);
    }

    @Test
    void getDashboardRejectsFutureDateRange() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        PlatformRevenueDashboardRequest request = buildRequest(
            tomorrow,
            tomorrow,
            RevenueGranularity.DAY
        );

        assertThrows(
            BusinessException.class,
            () -> platformRevenueService.getDashboard(request)
        );
        verifyNoInteractions(platformRevenueRepository);
    }

    @Test
    void getDashboardRejectsReportingPeriodOver366Days() {
        LocalDate toDate = LocalDate.now();
        PlatformRevenueDashboardRequest request = buildRequest(
            toDate.minusDays(366),
            toDate,
            RevenueGranularity.DAY
        );

        assertThrows(
            BusinessException.class,
            () -> platformRevenueService.getDashboard(request)
        );
        verifyNoInteractions(platformRevenueRepository);
    }

    private PlatformRevenueDashboardRequest buildRequest(
        LocalDate fromDate,
        LocalDate toDate,
        RevenueGranularity granularity
    ) {
        PlatformRevenueDashboardRequest request =
            new PlatformRevenueDashboardRequest();
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        request.setGranularity(granularity);
        request.setTopLimit(5);
        return request;
    }
}

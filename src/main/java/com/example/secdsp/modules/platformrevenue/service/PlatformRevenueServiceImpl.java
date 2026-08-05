package com.example.secdsp.modules.platformrevenue.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.order.entity.OrderStatus;
import com.example.secdsp.modules.payment.entity.PaymentMethod;
import com.example.secdsp.modules.platformrevenue.dto.request.PlatformRevenueDashboardRequest;
import com.example.secdsp.modules.platformrevenue.dto.request.RevenueGranularity;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.OrderStatusDistributionItem;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.Overview;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.PaymentMethodDistributionItem;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.Period;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.PlatformActivity;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.PlatformActivityTrendPoint;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.RevenueTrendPoint;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.TopCategoryItem;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.TopProductItem;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse.TopSellerItem;
import com.example.secdsp.modules.platformrevenue.repository.PlatformRevenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlatformRevenueServiceImpl implements PlatformRevenueService {

    private static final int MAX_REPORTING_DAYS = 366;
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final PlatformRevenueRepository platformRevenueRepository;

    @Override
    public PlatformRevenueDashboardResponse getDashboard(
        PlatformRevenueDashboardRequest request
    ) {
        validateRequest(request);

        LocalDate fromDate = request.getFromDate();
        LocalDate toDate = request.getToDate();
        LocalDateTime startDateTime = fromDate.atStartOfDay();
        LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay();

        long reportingDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDateTime previousStartDateTime =
            fromDate.minusDays(reportingDays).atStartOfDay();

        log.info(
            "Loading platform revenue dashboard from {} to {} with {} granularity",
            fromDate,
            toDate,
            request.getGranularity()
        );

        Object[] orderOverview = firstRow(
            platformRevenueRepository.findOrderOverview(
                startDateTime,
                endDateTime
            )
        );
        Object[] itemOverview = firstRow(
            platformRevenueRepository.findItemOverview(
                startDateTime,
                endDateTime
            )
        );

        BigDecimal grossMerchandiseValue = money(orderOverview, 0);
        BigDecimal previousPeriodGmv = money(
            platformRevenueRepository.findGrossMerchandiseValue(
                previousStartDateTime,
                startDateTime
            )
        );
        BigDecimal successfulPaymentAmount = money(
            platformRevenueRepository.findSuccessfulPaymentAmount(
                startDateTime,
                endDateTime
            )
        );

        Overview overview = buildOverview(
            orderOverview,
            itemOverview,
            grossMerchandiseValue,
            previousPeriodGmv,
            successfulPaymentAmount
        );

        return PlatformRevenueDashboardResponse.builder()
            .period(Period.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .granularity(request.getGranularity())
                .generatedAt(LocalDateTime.now())
                .build())
            .overview(overview)
            .orderStatusDistribution(buildOrderStatusDistribution(
                startDateTime,
                endDateTime,
                overview.totalOrders()
            ))
            .revenueTrend(buildRevenueTrend(
                request.getGranularity(),
                fromDate,
                toDate,
                startDateTime,
                endDateTime
            ))
            .topSellers(buildTopSellers(
                startDateTime,
                endDateTime,
                request.getTopLimit(),
                grossMerchandiseValue
            ))
            .topProducts(buildTopProducts(
                startDateTime,
                endDateTime,
                request.getTopLimit()
            ))
            .topCategories(buildTopCategories(
                startDateTime,
                endDateTime,
                request.getTopLimit(),
                grossMerchandiseValue
            ))
            .paymentMethodDistribution(buildPaymentMethodDistribution(
                startDateTime,
                endDateTime
            ))
            .platformActivity(buildPlatformActivity(
                startDateTime,
                endDateTime
            ))
            .activityTrend(buildActivityTrend(
                request.getGranularity(),
                fromDate,
                toDate,
                startDateTime,
                endDateTime
            ))
            .build();
    }

    private void validateRequest(PlatformRevenueDashboardRequest request) {
        if (request == null
            || request.getFromDate() == null
            || request.getToDate() == null) {
            throw new BusinessException("From date and to date are required.");
        }

        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new BusinessException("From date must not be after to date.");
        }

        if (request.getToDate().isAfter(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))) {
            throw new BusinessException("To date must not be in the future.");
        }

        long reportingDays = ChronoUnit.DAYS.between(
            request.getFromDate(),
            request.getToDate()
        ) + 1;

        if (reportingDays > MAX_REPORTING_DAYS) {
            throw new BusinessException(
                "Reporting period must not exceed 366 days."
            );
        }

        if (request.getGranularity() == null) {
            request.setGranularity(RevenueGranularity.DAY);
        }

        if (request.getTopLimit() == null) {
            request.setTopLimit(5);
        }

        if (request.getTopLimit() < 1 || request.getTopLimit() > 20) {
            throw new BusinessException("Top limit must be between 1 and 20.");
        }
    }

    private Overview buildOverview(
        Object[] orderOverview,
        Object[] itemOverview,
        BigDecimal grossMerchandiseValue,
        BigDecimal previousPeriodGmv,
        BigDecimal successfulPaymentAmount
    ) {
        long deliveredOrders = count(orderOverview, 5);

        return Overview.builder()
            .grossMerchandiseValue(grossMerchandiseValue)
            .previousPeriodGmv(previousPeriodGmv)
            .gmvGrowthPercentage(calculateGrowthPercentage(
                grossMerchandiseValue,
                previousPeriodGmv
            ))
            .successfulPaymentAmount(successfulPaymentAmount)
            .deliveredOrderValue(money(orderOverview, 1))
            .totalDiscountAmount(money(orderOverview, 2))
            .totalShippingFee(money(orderOverview, 3))
            .totalOrders(count(orderOverview, 4))
            .deliveredOrders(deliveredOrders)
            .averageOrderValue(divideMoney(
                money(orderOverview, 1),
                deliveredOrders
            ))
            .unitsSold(count(itemOverview, 0))
            .activeSellerCount(count(itemOverview, 1))
            .activeCustomerCount(count(orderOverview, 6))
            .build();
    }

    private List<OrderStatusDistributionItem> buildOrderStatusDistribution(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        long totalOrders
    ) {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        List<Object[]> rows = safeRows(
            platformRevenueRepository.findOrderStatusDistribution(
                startDateTime,
                endDateTime
            )
        );

        for (Object[] row : rows) {
            String statusName = String.valueOf(row[0]);
            try {
                counts.put(OrderStatus.valueOf(statusName), count(row, 1));
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping unknown order status in revenue report: {}", statusName);
            }
        }

        List<OrderStatusDistributionItem> distribution = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            long orderCount = counts.getOrDefault(status, 0L);
            distribution.add(OrderStatusDistributionItem.builder()
                .status(status)
                .orderCount(orderCount)
                .percentage(calculatePercentage(orderCount, totalOrders))
                .build());
        }
        return distribution;
    }

    private List<RevenueTrendPoint> buildRevenueTrend(
        RevenueGranularity granularity,
        LocalDate fromDate,
        LocalDate toDate,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    ) {
        List<Object[]> rows = granularity == RevenueGranularity.MONTH
            ? platformRevenueRepository.findMonthlyRevenueTrend(
                startDateTime,
                endDateTime
            )
            : platformRevenueRepository.findDailyRevenueTrend(
                startDateTime,
                endDateTime
            );

        Map<LocalDate, Object[]> valuesByPeriod = indexByPeriod(rows);
        List<RevenueTrendPoint> trend = new ArrayList<>();

        for (LocalDate periodStart : buildPeriods(
            fromDate,
            toDate,
            granularity
        )) {
            Object[] row = valuesByPeriod.get(periodStart);
            trend.add(RevenueTrendPoint.builder()
                .periodStart(periodStart)
                .grossMerchandiseValue(money(row, 1))
                .deliveredOrderValue(money(row, 2))
                .deliveredOrders(count(row, 3))
                .unitsSold(count(row, 4))
                .build());
        }
        return trend;
    }

    private List<TopSellerItem> buildTopSellers(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        int topLimit,
        BigDecimal grossMerchandiseValue
    ) {
        return safeRows(platformRevenueRepository.findTopSellers(
            startDateTime,
            endDateTime,
            topLimit
        )).stream()
            .map(row -> TopSellerItem.builder()
                .sellerId(count(row, 0))
                .sellerName(text(row, 1))
                .grossMerchandiseValue(money(row, 2))
                .deliveredOrders(count(row, 3))
                .unitsSold(count(row, 4))
                .marketSharePercentage(calculatePercentage(
                    money(row, 2),
                    grossMerchandiseValue
                ))
                .build())
            .toList();
    }

    private List<TopProductItem> buildTopProducts(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        int topLimit
    ) {
        return safeRows(platformRevenueRepository.findTopProducts(
            startDateTime,
            endDateTime,
            topLimit
        )).stream()
            .map(row -> TopProductItem.builder()
                .productId(count(row, 0))
                .productName(text(row, 1))
                .sellerId(count(row, 2))
                .sellerName(text(row, 3))
                .grossMerchandiseValue(money(row, 4))
                .deliveredOrders(count(row, 5))
                .unitsSold(count(row, 6))
                .build())
            .toList();
    }

    private List<TopCategoryItem> buildTopCategories(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        int topLimit,
        BigDecimal grossMerchandiseValue
    ) {
        return safeRows(platformRevenueRepository.findTopCategories(
            startDateTime,
            endDateTime,
            topLimit
        )).stream()
            .map(row -> TopCategoryItem.builder()
                .categoryId(nullableLong(row, 0))
                .categoryName(text(row, 1))
                .grossMerchandiseValue(money(row, 2))
                .deliveredOrders(count(row, 3))
                .unitsSold(count(row, 4))
                .marketSharePercentage(calculatePercentage(
                    money(row, 2),
                    grossMerchandiseValue
                ))
                .build())
            .toList();
    }

    private List<PaymentMethodDistributionItem>
    buildPaymentMethodDistribution(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    ) {
        Map<PaymentMethod, Object[]> values = new EnumMap<>(PaymentMethod.class);
        List<Object[]> rows = safeRows(
            platformRevenueRepository.findPaymentMethodDistribution(
                startDateTime,
                endDateTime
            )
        );

        for (Object[] row : rows) {
            String methodName = String.valueOf(row[0]);
            try {
                values.put(PaymentMethod.valueOf(methodName), row);
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping unknown payment method in revenue report: {}", methodName);
            }
        }

        BigDecimal cohortSuccessfulAmount = rows.stream()
            .map(row -> money(row, 5))
            .reduce(zeroMoney(), BigDecimal::add);

        List<PaymentMethodDistributionItem> distribution = new ArrayList<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            Object[] row = values.get(method);
            BigDecimal successfulAmount = money(row, 5);
            distribution.add(PaymentMethodDistributionItem.builder()
                .paymentMethod(method)
                .totalPaymentCount(count(row, 1))
                .successfulPaymentCount(count(row, 2))
                .pendingPaymentCount(count(row, 3))
                .failedPaymentCount(count(row, 4))
                .successfulAmount(successfulAmount)
                .percentage(calculatePercentage(
                    successfulAmount,
                    cohortSuccessfulAmount
                ))
                .build());
        }
        return distribution;
    }

    private PlatformActivity buildPlatformActivity(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    ) {
        Object[] users = firstRow(platformRevenueRepository.findUserActivity(
            startDateTime,
            endDateTime
        ));
        Object[] products = firstRow(platformRevenueRepository.findProductActivity(
            startDateTime,
            endDateTime
        ));

        return PlatformActivity.builder()
            .totalSellers(count(users, 0))
            .activeSellerAccounts(count(users, 1))
            .newSellers(count(users, 2))
            .totalCustomers(count(users, 3))
            .activeCustomerAccounts(count(users, 4))
            .newCustomers(count(users, 5))
            .totalProducts(count(products, 0))
            .activeProducts(count(products, 1))
            .inactiveProducts(count(products, 2))
            .outOfStockProducts(count(products, 3))
            .newProducts(count(products, 4))
            .totalCategories(platformRevenueRepository.countActiveCategories())
            .uncategorizedProducts(count(products, 5))
            .build();
    }

    private List<PlatformActivityTrendPoint> buildActivityTrend(
        RevenueGranularity granularity,
        LocalDate fromDate,
        LocalDate toDate,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    ) {
        List<Object[]> rows = granularity == RevenueGranularity.MONTH
            ? platformRevenueRepository.findMonthlyActivityTrend(
                startDateTime,
                endDateTime
            )
            : platformRevenueRepository.findDailyActivityTrend(
                startDateTime,
                endDateTime
            );

        Map<LocalDate, Object[]> valuesByPeriod = indexByPeriod(rows);
        List<PlatformActivityTrendPoint> trend = new ArrayList<>();

        for (LocalDate periodStart : buildPeriods(
            fromDate,
            toDate,
            granularity
        )) {
            Object[] row = valuesByPeriod.get(periodStart);
            trend.add(PlatformActivityTrendPoint.builder()
                .periodStart(periodStart)
                .newSellers(count(row, 1))
                .newCustomers(count(row, 2))
                .newProducts(count(row, 3))
                .build());
        }
        return trend;
    }

    private Map<LocalDate, Object[]> indexByPeriod(List<Object[]> rows) {
        Map<LocalDate, Object[]> valuesByPeriod = new HashMap<>();
        for (Object[] row : safeRows(rows)) {
            if (row.length == 0 || row[0] == null) {
                continue;
            }
            try {
                valuesByPeriod.put(toLocalDate(row[0]), row);
            } catch (Exception ex) {
                log.warn("Skipping revenue period row with bad period_start={}: {}",
                    row[0], ex.getMessage());
            }
        }
        return valuesByPeriod;
    }

    /** Native single-row queries must return List to avoid Hibernate Object[] wrapping bugs. */
    private Object[] firstRow(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return unwrapRow(rows.get(0));
    }

    /**
     * Hibernate/Spring sometimes returns a single native row as
     * {@code Object[]{ Object[]{ col0, col1, ... } }} — unwrap until flat.
     */
    private Object[] unwrapRow(Object raw) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Object[] arr)) {
            return new Object[] { raw };
        }
        Object[] current = arr;
        while (current.length == 1 && current[0] instanceof Object[] nested) {
            current = nested;
        }
        return current;
    }

    private List<LocalDate> buildPeriods(
        LocalDate fromDate,
        LocalDate toDate,
        RevenueGranularity granularity
    ) {
        List<LocalDate> periods = new ArrayList<>();
        LocalDate current = granularity == RevenueGranularity.MONTH
            ? fromDate.withDayOfMonth(1)
            : fromDate;

        while (!current.isAfter(toDate)) {
            periods.add(current);
            current = granularity == RevenueGranularity.MONTH
                ? current.plusMonths(1)
                : current.plusDays(1);
        }
        return periods;
    }

    private BigDecimal calculateGrowthPercentage(
        BigDecimal currentValue,
        BigDecimal previousValue
    ) {
        if (previousValue.signum() == 0) {
            return null;
        }

        return currentValue.subtract(previousValue)
            .multiply(ONE_HUNDRED)
            .divide(previousValue, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentage(long value, long total) {
        if (total == 0) {
            return zeroMoney();
        }

        return BigDecimal.valueOf(value)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(total), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentage(
        BigDecimal value,
        BigDecimal total
    ) {
        if (total.signum() == 0) {
            return zeroMoney();
        }

        return value.multiply(ONE_HUNDRED)
            .divide(total, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal divideMoney(BigDecimal value, long divisor) {
        if (divisor == 0) {
            return zeroMoney();
        }
        return value.divide(
            BigDecimal.valueOf(divisor),
            MONEY_SCALE,
            RoundingMode.HALF_UP
        );
    }

    private BigDecimal money(Object[] row, int index) {
        return row == null || row.length <= index
            ? zeroMoney()
            : money(row[index]);
    }

    private BigDecimal money(Object value) {
        if (value == null) {
            return zeroMoney();
        }
        // Never attempt BigDecimal.parse on temporal types ("2026-07-07" → NFE missing "e")
        if (value instanceof LocalDate
            || value instanceof LocalDateTime
            || value instanceof Date
            || value instanceof Timestamp
            || value instanceof java.util.Date) {
            log.warn("money() received temporal value {}; treating as 0", value);
            return zeroMoney();
        }
        if (value instanceof Object[]) {
            log.warn("money() received nested Object[]; treating as 0");
            return zeroMoney();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            if (number instanceof Double || number instanceof Float) {
                return BigDecimal.valueOf(number.doubleValue())
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            try {
                return new BigDecimal(number.toString())
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            } catch (NumberFormatException ex) {
                return BigDecimal.valueOf(number.doubleValue())
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
        }
        String raw = value.toString().trim();
        if (raw.isEmpty()) {
            return zeroMoney();
        }
        try {
            return new BigDecimal(raw).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            log.warn("money() could not parse '{}': {}", raw, ex.getMessage());
            return zeroMoney();
        }
    }

    private BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private long count(Object[] row, int index) {
        Long value = nullableLong(row, index);
        return value == null ? 0L : value;
    }

    private Long nullableLong(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return null;
        }
        Object value = row[index];
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException ex) {
            log.warn("nullableLong() could not parse '{}'", value);
            return null;
        }
    }

    private String text(Object[] row, int index) {
        return row == null || row.length <= index || row[index] == null
            ? null
            : row[index].toString();
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("period_start is null");
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return new Timestamp(utilDate.getTime()).toLocalDateTime().toLocalDate();
        }
        String raw = value.toString().trim();
        // Postgres may return "2026-07-07 00:00:00" from DATE_TRUNC cast
        if (raw.length() >= 10 && raw.charAt(4) == '-' && raw.charAt(7) == '-') {
            return LocalDate.parse(raw.substring(0, 10));
        }
        return LocalDate.parse(raw);
    }

    private List<Object[]> safeRows(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Object[]> out = new ArrayList<>(rows.size());
        for (Object raw : rows) {
            Object[] row = unwrapRow(raw);
            if (row != null) {
                out.add(row);
            }
        }
        return out;
    }
}

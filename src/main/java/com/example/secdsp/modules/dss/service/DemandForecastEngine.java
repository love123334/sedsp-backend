package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastComputation;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastProductView;
import com.example.secdsp.modules.inventory.entity.Inventory;
import com.example.secdsp.modules.inventory.repository.InventoryRepository;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
@RequiredArgsConstructor
public class DemandForecastEngine {

    private static final String BASELINE_METHOD = "trend_blended_feature_forecast";
    private static final String LIGHTGBM_METHOD = "lightgbm_onnx";
    private static final String HYBRID_METHOD = "lightgbm_onnx_with_baseline_fallback";
    private static final int MIN_HISTORY_DAYS = 7;
    private static final int MAX_HISTORY_DAYS = 180;
    private static final int MIN_FORECAST_DAYS = 1;
    private static final int MAX_FORECAST_DAYS = 90;
    private static final int MIN_SIGNAL_DAYS = 3;

    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductReviewRepository productReviewRepository;
    private final LightGbmOnnxDemandPredictor lightGbmPredictor;

    @Transactional(readOnly = true)
    public DemandForecastComputation forecast(
        DemandForecastProductView product,
        int historicalDays,
        int forecastDays
    ) {
        int hist = clamp(historicalDays, MIN_HISTORY_DAYS, MAX_HISTORY_DAYS);
        int horizon = clamp(forecastDays, MIN_FORECAST_DAYS, MAX_FORECAST_DAYS);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(hist - 1L);

        return forecastBetween(product, startDate, endDate, hist, horizon);
    }

    @Transactional(readOnly = true)
    public DemandForecastComputation forecast(
        DemandForecastProductView product,
        LocalDate fromDate,
        LocalDate toDate,
        int forecastDays
    ) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException("Khoảng dữ liệu lịch sử không hợp lệ.");
        }

        long requestedHistoryDays = ChronoUnit.DAYS.between(
            fromDate,
            toDate
        ) + 1L;
        if (requestedHistoryDays < MIN_HISTORY_DAYS
            || requestedHistoryDays > MAX_HISTORY_DAYS) {
            throw new BusinessException(
                "Khoảng dữ liệu lịch sử phải từ 7 đến 180 ngày."
            );
        }

        if (forecastDays < MIN_FORECAST_DAYS
            || forecastDays > MAX_FORECAST_DAYS) {
            throw new BusinessException(
                "Khoảng dự báo phải từ 1 đến 90 ngày."
            );
        }

        return forecastBetween(
            product,
            fromDate,
            toDate,
            Math.toIntExact(requestedHistoryDays),
            forecastDays
        );
    }

    private DemandForecastComputation forecastBetween(
        DemandForecastProductView product,
        LocalDate startDate,
        LocalDate endDate,
        int hist,
        int horizon
    ) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1L).atStartOfDay();

        Map<LocalDate, Long> soldByDate = new LinkedHashMap<>();
        orderItemRepository.findCompletedDailySalesByProduct(
                product.productId(),
                startDateTime,
                endDateTime
            )
            .forEach(row -> {
                LocalDate saleDate = row[0] instanceof java.sql.Date sqlDate
                    ? sqlDate.toLocalDate()
                    : (LocalDate) row[0];
                long quantity = ((Number) row[1]).longValue();
                soldByDate.put(saleDate, quantity);
            });

        List<Long> dailySeries = new ArrayList<>();
        List<Map<String, Object>> historicalSales = new ArrayList<>();
        long totalHistoricalQuantity = 0;
        long positiveDays = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long quantity = soldByDate.getOrDefault(date, 0L);
            totalHistoricalQuantity += quantity;
            if (quantity > 0) {
                positiveDays++;
            }

            dailySeries.add(quantity);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.toString());
            point.put("qty", quantity);
            historicalSales.add(point);
        }

        int recentWindow = Math.min(7, dailySeries.size());
        int mediumWindow = Math.min(14, dailySeries.size());
        double recentAverage = averageOfTail(dailySeries, recentWindow);
        double mediumAverage = averageOfTail(dailySeries, mediumWindow);
        double longAverage = dailySeries.isEmpty()
            ? 0.0
            : totalHistoricalQuantity / (double) dailySeries.size();
        double previousAverage = averageOfPreviousTail(dailySeries, recentWindow);
        double momentum = recentAverage - previousAverage;
        double slope = linearRegressionSlope(dailySeries);
        double lag7 = dailySeries.size() > 7
            ? dailySeries.get(dailySeries.size() - 8)
            : recentAverage;
        double seasonalSignal = lag7 - recentAverage;

        boolean insufficientData =
            totalHistoricalQuantity <= 0 || positiveDays < MIN_SIGNAL_DAYS;
        boolean onnxModelAvailable = lightGbmPredictor
            .isModelAvailable(product.productId());

        Map<String, Object> featureSnapshot = buildFeatureSnapshot(
            product,
            hist,
            horizon,
            totalHistoricalQuantity,
            positiveDays,
            recentAverage,
            mediumAverage,
            longAverage,
            previousAverage,
            momentum,
            slope,
            lag7,
            seasonalSignal,
            insufficientData,
            BASELINE_METHOD,
            onnxModelAvailable
        );

        if (insufficientData) {
            return new DemandForecastComputation(
                product.productId(),
                product.productName(),
                hist,
                horizon,
                0.0,
                0L,
                BASELINE_METHOD,
                true,
                historicalSales,
                List.of(),
                featureSnapshot,
                now()
            );
        }

        double baseLevel = (recentAverage * 0.55)
            + (mediumAverage * 0.30)
            + (longAverage * 0.15);

        List<Map<String, Object>> forecastSales = new ArrayList<>();
        List<Long> recursiveHistory = new ArrayList<>(dailySeries);
        double forecastTotal = 0.0;
        boolean usedOnnxModel = false;
        boolean usedFallback = false;

        for (int horizonIndex = 1; horizonIndex <= horizon; horizonIndex++) {
            LocalDate targetDate = endDate.plusDays(horizonIndex);
            OptionalDouble modelPrediction = onnxModelAvailable
                ? lightGbmPredictor.predict(
                    product.productId(),
                    targetDate,
                    recursiveHistory,
                    hist
                )
                : OptionalDouble.empty();

            double predictedValue;
            if (modelPrediction.isPresent()) {
                predictedValue = modelPrediction.getAsDouble();
                usedOnnxModel = true;
            } else {
                double decay = Math.exp(
                    -((double) (horizonIndex - 1) / Math.max(3.0, recentWindow))
                );
                predictedValue = baseLevel
                    + (slope * horizonIndex)
                    + (momentum * 0.35 * decay)
                    + (seasonalSignal * 0.20 * decay);
                usedFallback = usedFallback || onnxModelAvailable;
            }

            long predictedQty = Math.max(0L, Math.round(predictedValue));
            forecastTotal += predictedQty;
            recursiveHistory.add(predictedQty);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("day", horizonIndex);
            point.put("date", targetDate.toString());
            point.put("qty", predictedQty);
            forecastSales.add(point);
        }

        double forecastAverage = forecastTotal / horizon;
        String method = usedOnnxModel
            ? (usedFallback ? HYBRID_METHOD : LIGHTGBM_METHOD)
            : BASELINE_METHOD;

        featureSnapshot.put("method", method);
        featureSnapshot.put("onnxModelUsed", usedOnnxModel);
        featureSnapshot.put(
            "onnxModelAvailable",
            lightGbmPredictor.isModelAvailable(product.productId())
        );
        featureSnapshot.put("baseForecastDailyDemand", round2(baseLevel));
        featureSnapshot.put("forecastAverageDailyDemand", round2(forecastAverage));

        return new DemandForecastComputation(
            product.productId(),
            product.productName(),
            hist,
            horizon,
            round2(forecastAverage),
            Math.round(forecastTotal),
            method,
            false,
            historicalSales,
            forecastSales,
            featureSnapshot,
            now()
        );
    }

    private Map<String, Object> buildFeatureSnapshot(
        DemandForecastProductView product,
        int historicalDays,
        int forecastDays,
        long totalHistoricalQuantity,
        long positiveDays,
        double recentAverage,
        double mediumAverage,
        double longAverage,
        double previousAverage,
        double momentum,
        double slope,
        double lag7,
        double seasonalSignal,
        boolean insufficientData,
        String method,
        boolean onnxModelAvailable
    ) {
        Optional<Inventory> inventory = inventoryRepository
            .findByProduct_Id(product.productId());
        Object[] ratingSummary = productReviewRepository
            .getRatingSummary(product.productId());
        if (ratingSummary == null || ratingSummary.length == 0) {
            ratingSummary = new Object[] { null, 0L };
        }

        int availableQuantity = inventory.map(Inventory::getAvailableQuantity)
            .orElse(0);
        int reservedQuantity = inventory.map(Inventory::getReservedQuantity)
            .orElse(0);
        double averageRating = ratingSummary.length > 0
            && ratingSummary[0] instanceof Number value
            ? value.doubleValue()
            : 0.0;
        long reviewCount = ratingSummary.length > 1
            && ratingSummary[1] instanceof Number value
            ? value.longValue()
            : 0L;
        double stockCoverDays = recentAverage > 0
            ? availableQuantity / recentAverage
            : 0.0;

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("method", method);
        snapshot.put("onnxModelAvailable", onnxModelAvailable);
        snapshot.put("onnxModelUsed", false);
        snapshot.put("historicalDays", historicalDays);
        snapshot.put("forecastDays", forecastDays);
        snapshot.put("totalHistoricalQuantity", totalHistoricalQuantity);
        snapshot.put("positiveDays", positiveDays);
        snapshot.put("recentAverageDailyDemand", round2(recentAverage));
        snapshot.put("mediumAverageDailyDemand", round2(mediumAverage));
        snapshot.put("longAverageDailyDemand", round2(longAverage));
        snapshot.put("previousAverageDailyDemand", round2(previousAverage));
        snapshot.put("momentum", round2(momentum));
        snapshot.put("trendSlope", round4(slope));
        snapshot.put("lag7", round2(lag7));
        snapshot.put("seasonalSignal", round2(seasonalSignal));
        snapshot.put("currentPrice", product.currentPrice());
        snapshot.put("currentStock", availableQuantity);
        snapshot.put("reservedStock", reservedQuantity);
        snapshot.put("stockCoverDays", round2(stockCoverDays));
        snapshot.put("averageRating", round2(averageRating));
        snapshot.put("reviewCount", reviewCount);
        snapshot.put("insufficientData", insufficientData);
        return snapshot;
    }

    private static double averageOfTail(List<Long> series, int window) {
        if (series.isEmpty() || window <= 0) {
            return 0.0;
        }

        int start = Math.max(0, series.size() - window);
        long sum = 0L;
        for (int i = start; i < series.size(); i++) {
            sum += series.get(i);
        }
        return sum / (double) (series.size() - start);
    }

    private static double averageOfPreviousTail(List<Long> series, int window) {
        if (series.size() <= window || window <= 0) {
            return averageOfTail(series, window);
        }

        int end = series.size() - window;
        int start = Math.max(0, end - window);
        long sum = 0L;
        for (int i = start; i < end; i++) {
            sum += series.get(i);
        }
        return sum / (double) (end - start);
    }

    private static double linearRegressionSlope(List<Long> series) {
        if (series.size() < 2) {
            return 0.0;
        }

        int n = series.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i + 1.0;
            double y = series.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0.0) {
            return 0.0;
        }

        return ((n * sumXY) - (sumX * sumY)) / denominator;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private static double round4(double value) {
        return BigDecimal.valueOf(value)
            .setScale(4, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private static String now() {
        return LocalDateTime.now().toString();
    }
}

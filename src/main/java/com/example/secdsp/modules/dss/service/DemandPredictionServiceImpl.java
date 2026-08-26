package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.request.GenerateDemandPredictionRequest;
import com.example.secdsp.modules.dss.dto.response.DemandPredictionResponse;
import com.example.secdsp.modules.dss.dto.response.DssForecastDayResponse;
import com.example.secdsp.modules.dss.entity.DemandPrediction;
import com.example.secdsp.modules.dss.mapper.DemandPredictionMapper;
import com.example.secdsp.modules.dss.repository.DemandPredictionRepository;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.PriceHistoryInfo;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandPredictionServiceImpl
    implements DemandPredictionService {

    private static final String INSUFFICIENT_DATA_MESSAGE =
        "Không đủ dữ liệu để tạo dự báo.";

    private final DemandPredictionRepository demandPredictionRepository;
    private final DemandPredictionMapper demandPredictionMapper;
    private final ProductService productService;
    private final OrderService orderService;
    private final DssProductContextService productContextService;
    private final DssPredictionInsightService predictionInsightService;

    @Override
    @Transactional
    public DemandPredictionResponse generatePrediction(
        GenerateDemandPredictionRequest request
    ) {
        log.info(
            "Generating demand prediction for product {} with {} historical days and {} forecast days",
            request.getProductId(),
            request.getHistoricalDays(),
            request.getForecastPeriod()
        );

        ProductInfo product = productService.getProductInfo(request.getProductId());
        Long currentUserId = requireCurrentUserId();
        validateProductAccess(product, currentUserId);

        LocalDate firstSaleDate = orderService.getFirstCompletedSaleDate(product.id());
        if (firstSaleDate == null) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(request.getHistoricalDays() - 1L);
        if (firstSaleDate.isAfter(startDate)) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        Map<LocalDate, Long> dailySales = orderService.getCompletedDailySalesMap(
            product.id(),
            startDate,
            endDate
        );

        long totalQuantitySold = dailySales.values().stream()
            .mapToLong(Long::longValue)
            .sum();
        if (totalQuantitySold <= 0) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        DssForecastUtil.ForecastResult forecast = dailySales.size() >= 3
            ? DssForecastUtil.forecast(
                dailySales,
                startDate,
                endDate,
                request.getForecastPeriod()
            )
            : DssForecastUtil.simpleAverage(
                totalQuantitySold,
                request.getHistoricalDays(),
                request.getForecastPeriod(),
                startDate,
                endDate
            );

        List<PriceHistoryInfo> priceHistories = productService.getPriceHistoryInfo(
            product.id(),
            startDate,
            endDate
        );

        var productContext = productContextService.buildContext(
            product.id(),
            product.sellerId(),
            startDate,
            endDate,
            firstSaleDate,
            priceHistories
        );

        var priceImpacts = DssPriceImpactAnalyzer.analyze(
            priceHistories,
            dailySales,
            startDate,
            endDate
        );

        DemandPrediction prediction = DemandPrediction.builder()
            .product(buildProductRef(product.id()))
            .historicalDays(request.getHistoricalDays())
            .forecastPeriod(request.getForecastPeriod())
            .averageDailyDemand(forecast.averageDailyDemand())
            .predictedQuantity(forecast.seasonalityAdjustedQuantity())
            .generatedBy(buildUserRef(currentUserId))
            .build();

        DemandPrediction saved = demandPredictionRepository.save(prediction);

        DemandPredictionResponse response = demandPredictionMapper.toResponse(saved);
        response.setProductName(product.name());
        response.setHistoricalFrom(startDate);
        response.setHistoricalTo(endDate);
        LocalDate forecastFrom = endDate.plusDays(1);
        LocalDate forecastTo = endDate.plusDays(request.getForecastPeriod());
        response.setForecastFrom(forecastFrom);
        response.setForecastTo(forecastTo);
        response.setHistoricalPeriodLabel(
            "Dữ liệu lịch sử: " + startDate + " → " + endDate
        );
        response.setForecastPeriodLabel(
            "Phạm vi dự báo: " + forecastFrom + " → " + forecastTo
        );
        response.setMethodology(forecast.methodology());
        response.setTrendFactor(forecast.trendFactor());
        response.setPredictedDemand(forecast.predictedQuantity());
        response.setSeasonalityAdjustedDemand(forecast.seasonalityAdjustedQuantity());
        response.setHolidayAdjustmentFactor(BigDecimal.ONE);
        response.setForecastSeries(mapForecastSeries(forecast));
        response.setUpcomingHolidays(List.of());
        response.setProductContext(productContext);
        response.setPriceChangeImpacts(priceImpacts);
        response.setAiInsight(predictionInsightService.generateDemandInsight(
            buildDemandFactsBrief(product, forecast, productContext, priceImpacts)
        ));

        log.info(
            "Demand prediction {} generated for product {} (seasonal qty={})",
            saved.getId(),
            product.id(),
            forecast.seasonalityAdjustedQuantity()
        );
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public double predictDemand(Long productId, int simulationPeriod) {
        DemandPrediction latestPrediction = demandPredictionRepository
            .findTopByProduct_IdOrderByCreatedAtDesc(productId)
            .orElseThrow(() -> new BusinessException(INSUFFICIENT_DATA_MESSAGE));

        return latestPrediction.getAverageDailyDemand()
            .multiply(BigDecimal.valueOf(simulationPeriod))
            .doubleValue();
    }

    private static List<DssForecastDayResponse> mapForecastSeries(
        DssForecastUtil.ForecastResult forecast
    ) {
        return forecast.forecastSeries().stream()
            .map(p -> DssForecastDayResponse.builder()
                .date(p.date())
                .predictedQty(p.predictedQty())
                .holidayNote(null)
                .build())
            .toList();
    }

    private static String buildDemandFactsBrief(
        ProductInfo product,
        DssForecastUtil.ForecastResult forecast,
        com.example.secdsp.modules.dss.dto.response.DssProductContextResponse ctx,
        List<com.example.secdsp.modules.dss.dto.response.DssPriceChangeImpactResponse> impacts
    ) {
        String priceNote = impacts.isEmpty()
            ? "Chưa chỉnh giá trong kỳ."
            : impacts.get(impacts.size() - 1).getSummary();

        return String.format(
            """
            Sản phẩm: %s
            TB/ngày: %s; dự báo %s SP; có mùa vụ theo thứ %s SP
            Xu hướng: %s%%
            Ngữ cảnh shop: %s
            Chỉnh giá gần nhất: %s
            """,
            product.name(),
            forecast.averageDailyDemand(),
            forecast.predictedQuantity(),
            forecast.seasonalityAdjustedQuantity(),
            forecast.trendFactor().multiply(BigDecimal.valueOf(100)),
            ctx.getPerformanceSummary(),
            priceNote
        );
    }

    private Long requireCurrentUserId() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required.");
        }
        return currentUserId;
    }

    private void validateProductAccess(ProductInfo product, Long currentUserId) {
        if (!currentUserId.equals(product.sellerId())) {
            throw new ForbiddenException(
                "You do not have permission to generate a prediction for this product."
            );
        }
    }

    private Product buildProductRef(Long productId) {
        Product product = new Product();
        product.setId(productId);
        return product;
    }

    private User buildUserRef(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}

package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.config.DssProperties;
import com.example.secdsp.modules.dss.dto.internal.PriceRegimeInfo;
import com.example.secdsp.modules.dss.dto.response.DssProfitBreakdownResponse;
import com.example.secdsp.modules.dss.dto.request.CustomPriceScenarioRequest;
import com.example.secdsp.modules.dss.dto.request.GeneratePricePredictionRequest;
import com.example.secdsp.modules.dss.dto.response.CustomPriceScenarioResponse;
import com.example.secdsp.modules.dss.dto.response.DssPriceChangeImpactResponse;
import com.example.secdsp.modules.dss.dto.response.DssProductContextResponse;
import com.example.secdsp.modules.dss.dto.response.PricePredictionResponse;
import com.example.secdsp.modules.dss.dto.response.PriceScenarioResponse;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.PriceHistoryInfo;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.dto.response.PriceHistoryResponse;
import com.example.secdsp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricePredictionServiceImpl
    implements PricePredictionService {

    private static final String INSUFFICIENT_DATA_MESSAGE =
        "Không đủ dữ liệu để tạo khuyến nghị giá.";
    private static final int CALCULATION_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    private final ProductService productService;
    private final OrderService orderService;
    private final DssProperties dssProperties;
    private final DssScenarioEngine scenarioEngine;
    private final DssProductContextService productContextService;
    private final DssPredictionInsightService predictionInsightService;

    @Override
    @Transactional(readOnly = true)
    public PricePredictionResponse generatePrediction(
        GeneratePricePredictionRequest request
    ) {
        validateDateRange(request.getFromDate(), request.getToDate());

        ProductInfo product = DssCostSupport.normalizeProductCost(
            productService.getProductInfo(request.getProductId())
        );

        Long sellerId = requireCurrentUserId();
        validateProductOwnership(product, sellerId);

        log.info(
            "Generating price prediction for product {} from {} to {}",
            product.id(),
            request.getFromDate(),
            request.getToDate()
        );

        List<PriceHistoryInfo> priceHistories = productService
            .getPriceHistoryInfo(
                product.id(),
                request.getFromDate(),
                request.getToDate()
            );

        if (priceHistories.isEmpty()) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        List<PriceRegimeInfo> regimes = buildPriceRegimes(
            product.id(),
            request.getFromDate(),
            request.getToDate(),
            priceHistories
        );

        BigDecimal averageElasticity =
            calculateAverageElasticity(regimes);

        long totalQuantitySold = regimes.stream()
            .mapToLong(PriceRegimeInfo::quantitySold)
            .sum();

        if (totalQuantitySold <= 0) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        long historicalDays = ChronoUnit.DAYS.between(
            request.getFromDate(),
            request.getToDate()
        ) + 1;
        int forecastDays = dssProperties.getDefaultForecastDays();
        BigDecimal baseDailyDemand = BigDecimal.valueOf(totalQuantitySold)
            .divide(
                BigDecimal.valueOf(historicalDays),
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
            );

        DssProfitBreakdownResponse currentBreakdown = scenarioEngine.profitAt(
            product.price(),
            product.costPrice(),
            Math.round(baseDailyDemand.doubleValue() * forecastDays)
        );

        List<PriceScenarioResponse> scenarios = buildScenarios(
            product.price(),
            product.costPrice(),
            averageElasticity,
            baseDailyDemand,
            forecastDays,
            currentBreakdown.getNetProfit()
        );

        final PriceScenarioResponse maxProfitScenario = scenarios.stream()
            .max(Comparator.comparing(
                PriceScenarioResponse::getExpectedProfit
            ))
            .orElseThrow(() ->
                new BusinessException(INSUFFICIENT_DATA_MESSAGE));

        scenarios = scenarios.stream()
            .map(s -> s.toBuilder()
                .recommended(s.getPriceChangePercent()
                    .equals(maxProfitScenario.getPriceChangePercent()))
                .build())
            .toList();
        PriceScenarioResponse bestScenario = scenarios.stream()
            .filter(PriceScenarioResponse::getRecommended)
            .findFirst()
            .orElse(maxProfitScenario);

        String recommendation = buildRecommendation(bestScenario, forecastDays);
        String reason = buildRecommendationReason(
            bestScenario,
            averageElasticity,
            request.getFromDate(),
            request.getToDate(),
            forecastDays,
            totalQuantitySold
        );

        log.info(
            "Price prediction generated for product {} with best price change {}%",
            product.id(),
            bestScenario.getPriceChangePercent()
        );

        LocalDate firstSaleDate = orderService.getFirstCompletedSaleDate(product.id());
        Map<LocalDate, Long> dailySales = orderService.getCompletedDailySalesMap(
            product.id(),
            request.getFromDate(),
            request.getToDate()
        );
        DssProductContextResponse productContext = productContextService.buildContext(
            product.id(),
            sellerId,
            request.getFromDate(),
            request.getToDate(),
            firstSaleDate,
            priceHistories
        );
        List<DssPriceChangeImpactResponse> priceChangeImpacts =
            DssPriceImpactAnalyzer.analyze(
                priceHistories,
                dailySales,
                request.getFromDate(),
                request.getToDate()
            );
        LocalDate forecastStart = request.getToDate().plusDays(1);
        LocalDate forecastEnd = forecastStart.plusDays(forecastDays - 1L);

        String priceFacts = buildPriceFactsBrief(
            product,
            averageElasticity,
            bestScenario,
            productContext,
            priceChangeImpacts
        );

        return PricePredictionResponse.builder()
            .productId(product.id())
            .productName(product.name())
            .fromDate(request.getFromDate())
            .toDate(request.getToDate())
            .currentPrice(product.price())
            .cost(product.costPrice())
            .averageElasticity(averageElasticity)
            .totalQuantitySold(totalQuantitySold)
            .bestScenario(bestScenario)
            .scenarios(scenarios)
            .forecastPeriodDays(forecastDays)
            .historicalPeriodLabel(
                "Dữ liệu lịch sử: "
                    + request.getFromDate()
                    + " → "
                    + request.getToDate()
            )
            .forecastPeriodLabel(
                "Phạm vi dự báo: " + forecastStart + " → " + forecastEnd
            )
            .forecastFrom(forecastStart)
            .forecastTo(forecastEnd)
            .scenarioAssumptionNote(scenarioEngine.scenarioAssumptionNote())
            .recommendation(recommendation)
            .recommendationReason(reason)
            .currentSituationBreakdown(currentBreakdown)
            .productContext(productContext)
            .priceChangeImpacts(priceChangeImpacts)
            .upcomingHolidays(List.of())
            .aiInsight(predictionInsightService.generatePriceInsight(priceFacts))
            .build();
    }

    private static String buildPriceFactsBrief(
        ProductInfo product,
        BigDecimal elasticity,
        PriceScenarioResponse best,
        DssProductContextResponse ctx,
        List<DssPriceChangeImpactResponse> impacts
    ) {
        String impactStr = impacts.isEmpty()
            ? "Chưa có lần chỉnh giá trong kỳ."
            : impacts.get(impacts.size() - 1).getSummary();
        return String.format(
            """
            SP: %s | Giá hiện tại: %s | Elasticity TB: %s
            Kịch bản đề xuất: %s%% → giá %s, lợi nhuận kỳ vọng %s
            %s
            Chỉnh giá: %s
            """,
            product.name(),
            product.price(),
            elasticity,
            best.getPriceChangePercent(),
            best.getNewPrice(),
            best.getExpectedProfit(),
            ctx.getPerformanceSummary(),
            impactStr
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPriceScenarioResponse evaluateCustomPriceScenario(
        CustomPriceScenarioRequest request
    ) {
        validateDateRange(request.getFromDate(), request.getToDate());

        ProductInfo product = DssCostSupport.normalizeProductCost(
            productService.getProductInfo(request.getProductId())
        );
        Long sellerId = requireCurrentUserId();
        validateProductOwnership(product, sellerId);

        if (request.getCustomPrice().compareTo(product.costPrice()) <= 0) {
            throw new BusinessException(
                "Giá tùy chỉnh phải lớn hơn giá vốn (" + product.costPrice() + " VND)."
            );
        }

        List<PriceHistoryInfo> priceHistories = productService.getPriceHistoryInfo(
            product.id(),
            request.getFromDate(),
            request.getToDate()
        );
        if (priceHistories.isEmpty()) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        List<PriceRegimeInfo> regimes = buildPriceRegimes(
            product.id(),
            request.getFromDate(),
            request.getToDate(),
            priceHistories
        );
        BigDecimal averageElasticity = calculateAverageElasticity(regimes);
        long totalQuantitySold = regimes.stream()
            .mapToLong(PriceRegimeInfo::quantitySold)
            .sum();
        if (totalQuantitySold <= 0) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        long historicalDays = ChronoUnit.DAYS.between(
            request.getFromDate(),
            request.getToDate()
        ) + 1;
        int forecastDays = dssProperties.getDefaultForecastDays();
        BigDecimal baseDailyDemand = BigDecimal.valueOf(totalQuantitySold)
            .divide(BigDecimal.valueOf(historicalDays), CALCULATION_SCALE, RoundingMode.HALF_UP);

        BigDecimal customPrice = request.getCustomPrice()
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal priceChangeRate = customPrice
            .subtract(product.price())
            .divide(product.price(), CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal derivedPct = priceChangeRate
            .multiply(ONE_HUNDRED)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal demandChangeRate = averageElasticity.multiply(priceChangeRate);
        long predictedDemand = BigDecimal.valueOf(forecastDays)
            .multiply(baseDailyDemand)
            .multiply(BigDecimal.ONE.add(demandChangeRate))
            .max(BigDecimal.ZERO)
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();

        DssProfitBreakdownResponse currentBreakdown = scenarioEngine.profitAt(
            product.price(),
            product.costPrice(),
            Math.round(baseDailyDemand.doubleValue() * forecastDays)
        );
        DssProfitBreakdownResponse scenarioBreakdown = scenarioEngine.profitAt(
            customPrice,
            product.costPrice(),
            predictedDemand
        );

        int roundedPct = derivedPct.setScale(0, RoundingMode.HALF_UP).intValue();
        String label = roundedPct == 0
            ? "Giá tùy chỉnh"
            : (roundedPct > 0
                ? "Tăng giá ~" + roundedPct + "%"
                : "Giảm giá ~" + Math.abs(roundedPct) + "%");

        PriceScenarioResponse scenario = PriceScenarioResponse.builder()
            .priceChangePercent(roundedPct)
            .cost(product.costPrice())
            .newPrice(customPrice)
            .profitPerProduct(customPrice.subtract(product.costPrice())
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP))
            .predictedDemand(predictedDemand)
            .expectedProfit(scenarioBreakdown.getNetProfit())
            .expectedRevenue(scenarioBreakdown.getRevenue())
            .profitChangePercent(scenarioEngine.profitChangePercent(
                currentBreakdown.getNetProfit(),
                scenarioBreakdown.getNetProfit()
            ))
            .profitBreakdown(scenarioBreakdown)
            .scenarioLabel(label)
            .recommended(false)
            .build();

        String recommendation = "Kịch bản giá "
            + customPrice
            + " VND (~"
            + derivedPct
            + "% so với giá hiện tại) trong "
            + forecastDays
            + " ngày tới.";
        String reason = "Nhu cầu dự báo "
            + predictedDemand
            + " SP; LN ròng ~"
            + scenarioBreakdown.getNetProfit()
            + " VND. Co giãn "
            + averageElasticity.setScale(2, RoundingMode.HALF_UP)
            + ".";

        return CustomPriceScenarioResponse.builder()
            .productId(product.id())
            .productName(product.name())
            .currentPrice(product.price())
            .customPrice(customPrice)
            .derivedPriceChangePercent(derivedPct)
            .forecastPeriodDays(forecastDays)
            .forecastPeriodLabel(scenarioEngine.forecastPeriodLabel(forecastDays))
            .scenario(scenario)
            .recommendation(recommendation)
            .recommendationReason(reason)
            .build();
    }

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    @Transactional(readOnly = true)
    public double calculateElasticity(Long productId) {
        LocalDate firstSaleDate = orderService
            .getFirstCompletedSaleDate(productId);

        if (firstSaleDate == null) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        LocalDate currentDate = LocalDate.now();
        List<PriceHistoryInfo> priceHistories = productService
            .getPriceHistory(productId)
            .stream()
            .filter(history -> isWithinRange(
                history,
                firstSaleDate,
                currentDate
            ))
            .sorted(Comparator.comparing(
                PriceHistoryResponse::getChangedAt
            ))
            .map(history -> new PriceHistoryInfo(
                history.getOldPrice(),
                history.getNewPrice(),
                history.getChangedAt()
            ))
            .toList();

        if (priceHistories.isEmpty()) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        return calculateAverageElasticity(
            buildPriceRegimes(
                productId,
                firstSaleDate,
                currentDate,
                priceHistories
            )
        ).doubleValue();
    }

    private List<PriceRegimeInfo> buildPriceRegimes(
        Long productId,
        LocalDate fromDate,
        LocalDate toDate,
        List<PriceHistoryInfo> priceHistories
    ) {
        List<PriceRegimeInfo> regimes = new ArrayList<>();
        LocalDate regimeStart = fromDate;
        BigDecimal regimePrice = priceHistories.get(0).oldPrice();

        validateHistoryPrice(regimePrice);

        for (PriceHistoryInfo history : priceHistories) {
            validateHistoryPrice(history.newPrice());

            LocalDate changeDate = history.changedAt().toLocalDate();

            if (changeDate.isAfter(regimeStart)) {
                regimes.add(buildPriceRegime(
                    productId,
                    regimePrice,
                    regimeStart,
                    changeDate.minusDays(1)
                ));
            }

            regimeStart = changeDate;
            regimePrice = history.newPrice();
        }

        if (!regimeStart.isAfter(toDate)) {
            regimes.add(buildPriceRegime(
                productId,
                regimePrice,
                regimeStart,
                toDate
            ));
        }

        if (regimes.size() < 2) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        return regimes;
    }

    private PriceRegimeInfo buildPriceRegime(
        Long productId,
        BigDecimal price,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        long numberOfDays = ChronoUnit.DAYS
            .between(fromDate, toDate) + 1;
        long quantitySold = orderService
            .getCompletedQuantitySold(productId, fromDate, toDate);
        BigDecimal averageDailyDemand = BigDecimal
            .valueOf(quantitySold)
            .divide(
                BigDecimal.valueOf(numberOfDays),
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
            );

        return new PriceRegimeInfo(
            price,
            fromDate,
            toDate,
            quantitySold,
            averageDailyDemand
        );
    }

    private BigDecimal calculateAverageElasticity(
        List<PriceRegimeInfo> regimes
    ) {
        List<BigDecimal> elasticities = new ArrayList<>();

        for (int index = 1; index < regimes.size(); index++) {
            PriceRegimeInfo previous = regimes.get(index - 1);
            PriceRegimeInfo current = regimes.get(index);

            if (previous.averageDailyDemand()
                .compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal priceChange = current.price()
                .subtract(previous.price())
                .divide(
                    previous.price(),
                    CALCULATION_SCALE,
                    RoundingMode.HALF_UP
                );

            if (priceChange.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal demandChange = current.averageDailyDemand()
                .subtract(previous.averageDailyDemand())
                .divide(
                    previous.averageDailyDemand(),
                    CALCULATION_SCALE,
                    RoundingMode.HALF_UP
                );

            BigDecimal elasticity = demandChange
                .divide(
                    priceChange,
                    CALCULATION_SCALE,
                    RoundingMode.HALF_UP
                )
                .abs()
                .negate();

            elasticities.add(elasticity);
        }

        if (elasticities.isEmpty()) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        BigDecimal totalElasticity = elasticities.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalElasticity.divide(
            BigDecimal.valueOf(elasticities.size()),
            CALCULATION_SCALE,
            RoundingMode.HALF_UP
        );
    }

    private List<PriceScenarioResponse> buildScenarios(
        BigDecimal currentPrice,
        BigDecimal cost,
        BigDecimal averageElasticity,
        BigDecimal baseDailyDemand,
        int forecastDays,
        BigDecimal currentNetProfit
    ) {
        return dssProperties.getPriceChangePercentages().stream()
            .map(changePercent -> buildScenario(
                currentPrice,
                cost,
                averageElasticity,
                baseDailyDemand,
                forecastDays,
                currentNetProfit,
                changePercent
            ))
            .toList();
    }

    private PriceScenarioResponse buildScenario(
        BigDecimal currentPrice,
        BigDecimal cost,
        BigDecimal averageElasticity,
        BigDecimal baseDailyDemand,
        int forecastDays,
        BigDecimal currentNetProfit,
        int priceChangePercent
    ) {
        DssScenarioEngine.DemandEstimate demand = scenarioEngine.estimateDemand(
            baseDailyDemand,
            forecastDays,
            averageElasticity,
            priceChangePercent
        );
        BigDecimal newPrice = scenarioEngine.newPriceFromChange(
            currentPrice,
            priceChangePercent
        );
        long predictedDemand = demand.quantity();
        DssProfitBreakdownResponse breakdown = scenarioEngine.profitAt(
            newPrice,
            cost,
            predictedDemand
        );
        BigDecimal profitPerProduct = newPrice
            .subtract(cost)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal expectedProfit = breakdown.getNetProfit();
        BigDecimal profitChange = scenarioEngine.profitChangePercent(
            currentNetProfit,
            expectedProfit
        );

        String label = priceChangePercent == 0
            ? "Giá hiện tại"
            : (priceChangePercent > 0
                ? "Tăng giá " + priceChangePercent + "%"
                : "Giảm giá " + Math.abs(priceChangePercent) + "%");

        return PriceScenarioResponse.builder()
            .priceChangePercent(priceChangePercent)
            .cost(cost)
            .newPrice(newPrice)
            .profitPerProduct(profitPerProduct)
            .predictedDemand(predictedDemand)
            .expectedProfit(expectedProfit)
            .expectedRevenue(breakdown.getRevenue())
            .profitChangePercent(profitChange)
            .profitBreakdown(breakdown)
            .scenarioLabel(label)
            .recommended(false)
            .build();
    }

    private String buildRecommendation(PriceScenarioResponse best, int forecastDays) {
        if (best.getPriceChangePercent() == 0) {
            return "Khuyến nghị: giữ giá hiện tại trong "
                + forecastDays
                + " ngày tới.";
        }
        String direction = best.getPriceChangePercent() > 0 ? "tăng" : "giảm";
        return "Khuyến nghị: "
            + direction
            + " giá khoảng "
            + Math.abs(best.getPriceChangePercent())
            + "% (→ "
            + best.getNewPrice()
            + " VND) trong "
            + forecastDays
            + " ngày tới.";
    }

    private String buildRecommendationReason(
        PriceScenarioResponse best,
        BigDecimal elasticity,
        LocalDate fromDate,
        LocalDate toDate,
        int forecastDays,
        long totalSold
    ) {
        return "Dựa trên "
            + totalSold
            + " SP bán từ "
            + fromDate
            + " đến "
            + toDate
            + ", hệ số co giãn trung bình "
            + elasticity.setScale(2, RoundingMode.HALF_UP)
            + ", kịch bản «"
            + best.getScenarioLabel()
            + "» cho lợi nhuận ròng cao nhất (~"
            + best.getExpectedProfit()
            + " VND) với "
            + best.getPredictedDemand()
            + " SP dự kiến trong "
            + forecastDays
            + " ngày. "
            + scenarioEngine.scenarioAssumptionNote();
    }

    private void validateDateRange(
        LocalDate fromDate,
        LocalDate toDate
    ) {
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(
                "From date must be before or equal to to date."
            );
        }

        if (toDate.isAfter(LocalDate.now())) {
            throw new BusinessException(
                "To date cannot be in the future."
            );
        }
    }

    private boolean isWithinRange(
        PriceHistoryResponse history,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        if (history.getChangedAt() == null) {
            return false;
        }

        LocalDate changedDate = history.getChangedAt().toLocalDate();
        return !changedDate.isBefore(fromDate)
            && !changedDate.isAfter(toDate);
    }

    private void validateHistoryPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }
    }

    private Long requireCurrentUserId() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        return currentUserId;
    }

    private void validateProductOwnership(
        ProductInfo product,
        Long sellerId
    ) {
        if (!sellerId.equals(product.sellerId())) {
            throw new ForbiddenException(
                "You do not have permission to generate a price prediction for this product."
            );
        }
    }
}

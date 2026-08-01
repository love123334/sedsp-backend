package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.internal.PriceRegimeInfo;
import com.example.secdsp.modules.dss.dto.request.GeneratePricePredictionRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PricePredictionServiceImpl
    implements PricePredictionService {

    private static final String INSUFFICIENT_DATA_MESSAGE =
        "Không đủ dữ liệu để tạo khuyến nghị giá.";
    private static final int CALCULATION_SCALE = 4;
    private static final int MONEY_SCALE = 2;
    private static final List<Integer> PRICE_CHANGE_PERCENTAGES =
        List.of(-10, -5, 0, 5, 10);

    private final ProductService productService;
    private final OrderService orderService;

    @Override
    @Transactional(readOnly = true)
    public PricePredictionResponse generatePrediction(
        GeneratePricePredictionRequest request
    ) {
        validateDateRange(request.getFromDate(), request.getToDate());

        ProductInfo product = productService
            .getProductInfo(request.getProductId());

        Long sellerId = requireCurrentUserId();
        validateProductOwnership(product, sellerId);
        validateProductPrices(product);

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

        List<PriceScenarioResponse> scenarios = buildScenarios(
            product.price(),
            product.costPrice(),
            averageElasticity,
            totalQuantitySold
        );

        PriceScenarioResponse bestScenario = scenarios.stream()
            .max(Comparator.comparing(
                PriceScenarioResponse::getExpectedProfit
            ))
            .orElseThrow(() ->
                new BusinessException(INSUFFICIENT_DATA_MESSAGE));

        log.info(
            "Price prediction generated for product {} with best price change {}%",
            product.id(),
            bestScenario.getPriceChangePercent()
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
            .build();
    }

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
        long totalQuantitySold
    ) {
        return PRICE_CHANGE_PERCENTAGES.stream()
            .map(changePercent -> buildScenario(
                currentPrice,
                cost,
                averageElasticity,
                totalQuantitySold,
                changePercent
            ))
            .toList();
    }

    private PriceScenarioResponse buildScenario(
        BigDecimal currentPrice,
        BigDecimal cost,
        BigDecimal averageElasticity,
        long totalQuantitySold,
        int priceChangePercent
    ) {
        BigDecimal priceChangeRate = BigDecimal
            .valueOf(priceChangePercent)
            .divide(
                BigDecimal.valueOf(100),
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
            );
        BigDecimal newPrice = currentPrice
            .multiply(BigDecimal.ONE.add(priceChangeRate))
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal demandChangeRate = averageElasticity
            .multiply(priceChangeRate);
        BigDecimal predictedDemandValue = BigDecimal
            .valueOf(totalQuantitySold)
            .multiply(BigDecimal.ONE.add(demandChangeRate))
            .max(BigDecimal.ZERO);
        long predictedDemand = predictedDemandValue
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();
        BigDecimal profitPerProduct = newPrice
            .subtract(cost)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal expectedProfit = profitPerProduct
            .multiply(BigDecimal.valueOf(predictedDemand))
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return PriceScenarioResponse.builder()
            .priceChangePercent(priceChangePercent)
            .cost(cost)
            .newPrice(newPrice)
            .profitPerProduct(profitPerProduct)
            .predictedDemand(predictedDemand)
            .expectedProfit(expectedProfit)
            .build();
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

    private void validateProductPrices(ProductInfo product) {
        if (product.price() == null
            || product.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                "Product selling price must be greater than 0."
            );
        }

        if (product.costPrice() == null
            || product.costPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                "Product cost is required to generate a price prediction."
            );
        }
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

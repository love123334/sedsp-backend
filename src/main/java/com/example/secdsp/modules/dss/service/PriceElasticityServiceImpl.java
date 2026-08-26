package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.util.AppTime;
import com.example.secdsp.modules.dss.dto.internal.PriceElasticitySnapshot;
import com.example.secdsp.modules.dss.dto.internal.PriceRegimeInfo;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.PriceHistoryInfo;
import com.example.secdsp.modules.product.dto.response.PriceHistoryResponse;
import com.example.secdsp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceElasticityServiceImpl implements PriceElasticityService {

    private static final String INSUFFICIENT_DATA_MESSAGE =
        "Không đủ biến động giá và dữ liệu bán hàng để tính hệ số co giãn E.";
    private static final int CALCULATION_SCALE = 4;

    private final ProductService productService;
    private final OrderService orderService;

    @Override
    @Transactional(readOnly = true)
    public PriceElasticitySnapshot analyze(
        Long productId,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        validateDateRange(fromDate, toDate);
        List<PriceHistoryInfo> histories = productService.getPriceHistoryInfo(
            productId,
            fromDate,
            toDate
        );
        return calculate(productId, fromDate, toDate, histories);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceElasticitySnapshot analyzeAllHistory(Long productId) {
        LocalDate firstSaleDate = orderService.getFirstCompletedSaleDate(productId);
        if (firstSaleDate == null) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        LocalDate toDate = AppTime.today();
        List<PriceHistoryInfo> histories = productService.getPriceHistory(productId)
            .stream()
            .filter(history -> isWithinRange(history, firstSaleDate, toDate))
            .sorted(Comparator.comparing(PriceHistoryResponse::getChangedAt))
            .map(history -> new PriceHistoryInfo(
                history.getOldPrice(),
                history.getNewPrice(),
                history.getChangedAt()
            ))
            .toList();

        return calculate(productId, firstSaleDate, toDate, histories);
    }

    private PriceElasticitySnapshot calculate(
        Long productId,
        LocalDate fromDate,
        LocalDate toDate,
        List<PriceHistoryInfo> histories
    ) {
        if (histories.isEmpty()) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }
        List<PriceRegimeInfo> regimes = buildPriceRegimes(
            productId,
            fromDate,
            toDate,
            histories
        );
        long quantitySold = regimes.stream()
            .mapToLong(PriceRegimeInfo::quantitySold)
            .sum();
        if (quantitySold <= 0) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }
        return new PriceElasticitySnapshot(
            calculateAverageElasticity(regimes),
            quantitySold
        );
    }

    private List<PriceRegimeInfo> buildPriceRegimes(
        Long productId,
        LocalDate fromDate,
        LocalDate toDate,
        List<PriceHistoryInfo> histories
    ) {
        List<PriceRegimeInfo> regimes = new ArrayList<>();
        LocalDate regimeStart = fromDate;
        BigDecimal regimePrice = histories.get(0).oldPrice();
        validateHistoryPrice(regimePrice);

        for (PriceHistoryInfo history : histories) {
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
            regimes.add(buildPriceRegime(productId, regimePrice, regimeStart, toDate));
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
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        long quantitySold = orderService.getCompletedQuantitySold(
            productId,
            fromDate,
            toDate
        );
        BigDecimal averageDailyDemand = BigDecimal.valueOf(quantitySold)
            .divide(
                BigDecimal.valueOf(days),
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

    private BigDecimal calculateAverageElasticity(List<PriceRegimeInfo> regimes) {
        List<BigDecimal> elasticities = new ArrayList<>();
        for (int index = 1; index < regimes.size(); index++) {
            PriceRegimeInfo previous = regimes.get(index - 1);
            PriceRegimeInfo current = regimes.get(index);
            if (previous.averageDailyDemand().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal priceChange = current.price()
                .subtract(previous.price())
                .divide(previous.price(), CALCULATION_SCALE, RoundingMode.HALF_UP);
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
            elasticities.add(
                demandChange.divide(
                    priceChange,
                    CALCULATION_SCALE,
                    RoundingMode.HALF_UP
                ).abs().negate()
            );
        }
        if (elasticities.isEmpty()) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }
        BigDecimal total = elasticities.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(
            BigDecimal.valueOf(elasticities.size()),
            CALCULATION_SCALE,
            RoundingMode.HALF_UP
        );
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate) || toDate.isAfter(AppTime.today())) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }
    }

    private void validateHistoryPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
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
        LocalDate date = history.getChangedAt().toLocalDate();
        return !date.isBefore(fromDate) && !date.isAfter(toDate);
    }
}

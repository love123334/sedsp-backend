package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.config.DssProperties;
import com.example.secdsp.modules.dss.dto.request.SalesQuantityTargetRequest;
import com.example.secdsp.modules.dss.dto.request.SellerDiscountAnalysisRequest;
import com.example.secdsp.modules.dss.dto.request.TargetProfitAnalysisRequest;
import com.example.secdsp.modules.dss.dto.response.DssProfitBreakdownResponse;
import com.example.secdsp.modules.dss.dto.response.SalesQuantityTargetResponse;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import com.example.secdsp.modules.dss.dto.response.TargetProfitAnalysisResponse;
import com.example.secdsp.modules.dss.entity.DemandPrediction;
import com.example.secdsp.modules.dss.repository.DemandPredictionRepository;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerWhatIfAnalysisServiceImpl
    implements SellerWhatIfAnalysisService {

    private final ProductService productService;
    private final DemandPredictionService demandPredictionService;
    private final PricePredictionService pricePredictionService;
    private final SellerDiscountProfitCalculator calculator;
    private final DssScenarioEngine scenarioEngine;
    private final DssProperties dssProperties;
    private final DemandPredictionRepository demandPredictionRepository;

    @Override
    @Transactional(readOnly = true)
    public SellerDiscountAnalysisResponse analyzeDiscount(
        SellerDiscountAnalysisRequest request
    ) {
        ProductInfo product = loadOwnedProduct(request.getProductId());

        log.info(
            "Analyzing {}% price change for product {} over {} days",
            request.getPriceChangePercent(),
            product.id(),
            request.getSimulationPeriod()
        );

        DemandPrediction latest = demandPredictionRepository
            .findTopByProduct_IdOrderByCreatedAtDesc(product.id())
            .orElse(null);

        double forecastDemand = demandPredictionService.predictDemand(
            product.id(),
            request.getSimulationPeriod()
        );
        double elasticity = pricePredictionService
            .calculateElasticity(product.id());

        String methodology = latest != null
            ? "Dự báo nhu cầu từ bản ghi DSS gần nhất ("
                + latest.getHistoricalDays()
                + " ngày lịch sử, TB "
                + latest.getAverageDailyDemand()
                + " SP/ngày)."
            : "Dự báo nhu cầu từ dữ liệu bán hàng DELIVERED.";

        String historicalLabel = latest != null
            ? "Lịch sử phân tích: " + latest.getHistoricalDays() + " ngày"
            : "Lịch sử: theo dự báo nhu cầu đã lưu";

        SellerDiscountAnalysisResponse response = calculator.calculate(
            product.price(),
            product.costPrice(),
            request.getPriceChangePercent(),
            forecastDemand,
            elasticity,
            request.getSimulationPeriod(),
            historicalLabel,
            methodology
        );

        log.info(
            "Discount analysis completed for product {} with expected profit {}",
            product.id(),
            response.getExpectedProfit()
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TargetProfitAnalysisResponse analyzeTargetProfit(
        TargetProfitAnalysisRequest request
    ) {
        ProductInfo product = loadOwnedProduct(request.getProductId());
        int period = request.getSimulationPeriod();
        long forecastDemand = Math.round(
            demandPredictionService.predictDemand(product.id(), period)
        );
        double elasticity = pricePredictionService.calculateElasticity(product.id());

        DssProfitBreakdownResponse currentSituation = scenarioEngine.profitAt(
            product.price(),
            product.costPrice(),
            forecastDemand
        );

        List<Integer> percents = dssProperties.getPriceChangePercentages();
        record Candidate(int pct, DssProfitBreakdownResponse breakdown, long qty) {}

        List<Candidate> candidates = percents.stream()
            .map(pct -> {
                var est = scenarioEngine.estimateDemand(
                    BigDecimal.valueOf(forecastDemand)
                        .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP),
                    period,
                    BigDecimal.valueOf(elasticity),
                    pct
                );
                BigDecimal newPrice = scenarioEngine.newPriceFromChange(
                    product.price(),
                    pct
                );
                return new Candidate(
                    pct,
                    scenarioEngine.profitAt(newPrice, product.costPrice(), est.quantity()),
                    est.quantity()
                );
            })
            .toList();

        Candidate best = candidates.stream()
            .max(Comparator.comparing(c -> c.breakdown().getNetProfit()))
            .orElseThrow(() -> new BusinessException("Không tạo được kịch bản."));

        BigDecimal gap = request.getTargetProfitVnd()
            .subtract(best.breakdown().getNetProfit());
        boolean achievable = best.breakdown().getNetProfit()
            .compareTo(request.getTargetProfitVnd()) >= 0;

        String recommendation = achievable
            ? "Khuyến nghị: "
                + (best.pct() == 0 ? "giữ giá hiện tại" : "điều chỉnh giá " + best.pct() + "%")
                + " — đạt hoặc vượt mục tiêu "
                + request.getTargetProfitVnd()
                + " VND lợi nhuận ròng trong "
                + period
                + " ngày."
            : "Mục tiêu "
                + request.getTargetProfitVnd()
                + " VND chưa đạt được với dải kịch bản cấu hình. Kịch bản gần nhất: "
                + best.pct()
                + "% (LN ròng ~"
                + best.breakdown().getNetProfit()
                + " VND, thiếu ~"
                + gap.abs()
                + " VND).";

        return TargetProfitAnalysisResponse.builder()
            .productId(product.id())
            .productName(product.name())
            .simulationPeriod(period)
            .forecastPeriodLabel(scenarioEngine.forecastPeriodLabel(period))
            .historicalPeriodLabel("Theo dự báo nhu cầu DSS đã lưu")
            .targetProfitVnd(request.getTargetProfitVnd())
            .currentPrice(product.price())
            .forecastDemand(forecastDemand)
            .currentSituation(currentSituation)
            .recommendedPriceChangePercent(best.pct())
            .recommendedPrice(scenarioEngine.newPriceFromChange(product.price(), best.pct()))
            .estimatedDemand(best.qty())
            .targetSituation(best.breakdown())
            .profitGapVnd(gap)
            .achievable(achievable)
            .recommendation(recommendation)
            .recommendationReason(
                "So sánh "
                    + percents.size()
                    + " kịch bản % giá; chọn LN ròng cao nhất. "
                    + scenarioEngine.scenarioAssumptionNote()
            )
            .methodology("Co giãn " + elasticity + ", nhu cầu gốc " + forecastDemand + " SP/" + period + " ngày.")
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesQuantityTargetResponse analyzeSalesQuantityTarget(
        SalesQuantityTargetRequest request
    ) {
        ProductInfo product = loadOwnedProduct(request.getProductId());
        int period = request.getSimulationPeriod();
        long currentQty = Math.round(
            demandPredictionService.predictDemand(product.id(), period)
        );
        double elasticity = pricePredictionService.calculateElasticity(product.id());

        long targetQty = Math.round(
            currentQty * (1.0 + request.getIncreasePercent() / 100.0)
        );
        if (currentQty <= 0) {
            throw new BusinessException("Không đủ dữ liệu nhu cầu để phân tích mục tiêu số lượng.");
        }

        double qtyIncreaseRate = (double) (targetQty - currentQty) / currentQty;
        BigDecimal elasticityBd = BigDecimal.valueOf(elasticity);
        BigDecimal requiredPriceChangeRate = elasticityBd.compareTo(BigDecimal.ZERO) != 0
            ? BigDecimal.valueOf(qtyIncreaseRate)
                .divide(elasticityBd, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        int priceChangePct = requiredPriceChangeRate
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();

        BigDecimal suggestedPrice = scenarioEngine.newPriceFromChange(
            product.price(),
            priceChangePct
        );

        DssProfitBreakdownResponse currentSituation = scenarioEngine.profitAt(
            product.price(),
            product.costPrice(),
            currentQty
        );
        DssProfitBreakdownResponse targetSituation = scenarioEngine.profitAt(
            suggestedPrice,
            product.costPrice(),
            targetQty
        );

        BigDecimal profitChange = scenarioEngine.profitChangePercent(
            currentSituation.getNetProfit(),
            targetSituation.getNetProfit()
        );

        String recommendation = priceChangePct < 0
            ? "Để tăng ~"
                + request.getIncreasePercent()
                + "% số lượng bán, ước tính cần giảm giá khoảng "
                + Math.abs(priceChangePct)
                + "% (→ "
                + suggestedPrice
                + " VND) trong "
                + period
                + " ngày."
            : (priceChangePct > 0
                ? "Mục tiêu +"
                    + request.getIncreasePercent()
                    + "% số lượng có thể cần tăng giá "
                    + priceChangePct
                    + "% — kiểm tra lại co giãn cầu."
                : "Giữ giá hiện tại có thể đủ cho mục tiêu số lượng trong kỳ dự báo.");

        return SalesQuantityTargetResponse.builder()
            .productId(product.id())
            .productName(product.name())
            .simulationPeriod(period)
            .forecastPeriodLabel(scenarioEngine.forecastPeriodLabel(period))
            .increasePercent(request.getIncreasePercent())
            .currentForecastQuantity(currentQty)
            .targetQuantity(targetQty)
            .currentPrice(product.price())
            .suggestedPrice(suggestedPrice)
            .suggestedPriceChangePercent(BigDecimal.valueOf(priceChangePct))
            .currentSituation(currentSituation)
            .targetSituation(targetSituation)
            .profitChangePercent(profitChange)
            .recommendation(recommendation)
            .recommendationReason(
                "Tăng số lượng "
                    + currentQty
                    + " → "
                    + targetQty
                    + " SP; co giãn "
                    + elasticity
                    + " → điều chỉnh giá "
                    + priceChangePct
                    + "%."
            )
            .methodology("Nhu cầu gốc từ dự báo DSS × kỳ " + period + " ngày.")
            .build();
    }

    private ProductInfo loadOwnedProduct(Long productId) {
        ProductInfo product = productService.getProductInfo(productId);
        Long sellerId = requireCurrentUserId();
        validateProductOwnership(product, sellerId);
        return DssCostSupport.normalizeProductCost(product);
    }

    private void validateProductPrices(ProductInfo product) {
        DssCostSupport.normalizeProductCost(product);
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
                "Bạn không có quyền phân tích sản phẩm này."
            );
        }
    }
}

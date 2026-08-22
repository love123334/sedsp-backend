package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastComputation;
import com.example.secdsp.modules.dss.dto.internal.DemandForecastProductView;
import com.example.secdsp.modules.dss.dto.internal.PriceElasticitySnapshot;
import com.example.secdsp.modules.dss.dto.request.CreateAdvancedPriceScenarioRequest;
import com.example.secdsp.modules.dss.dto.request.CreateAdvancedPriceSessionRequest;
import com.example.secdsp.modules.dss.dto.response.AdvancedPriceProductSummaryResponse;
import com.example.secdsp.modules.dss.dto.response.AdvancedPriceScenarioResponse;
import com.example.secdsp.modules.dss.dto.response.AdvancedPriceSessionResponse;
import com.example.secdsp.modules.dss.dto.response.ApplyAdvancedPriceScenarioResponse;
import com.example.secdsp.modules.dss.entity.AdvancedPriceScenario;
import com.example.secdsp.modules.dss.entity.AdvancedPriceSession;
import com.example.secdsp.modules.dss.entity.AdvancedPriceSessionStatus;
import com.example.secdsp.modules.dss.repository.AdvancedPriceScenarioRepository;
import com.example.secdsp.modules.dss.repository.AdvancedPriceSessionRepository;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.dto.request.UpdateProductRequest;
import com.example.secdsp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedPriceAnalysisServiceImpl
    implements AdvancedPriceAnalysisService {

    private static final int MAX_SCENARIOS = 5;
    private static final int MIN_HISTORY_DAYS = 7;
    private static final int MAX_HISTORY_DAYS = 180;
    private static final Set<Integer> ALLOWED_FORECAST_PERIODS =
        Set.of(7, 14, 30);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MIN_PRICE_CHANGE = BigDecimal.valueOf(-70);
    private static final BigDecimal MAX_PRICE_CHANGE = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;

    private static final String SELECTED_RANGE = "SELECTED_RANGE";
    private static final String ALL_HISTORY_FALLBACK = "ALL_HISTORY_FALLBACK";

    private final AdvancedPriceSessionRepository sessionRepository;
    private final AdvancedPriceScenarioRepository scenarioRepository;
    private final ProductService productService;
    private final OrderService orderService;
    private final PriceElasticityService priceElasticityService;
    private final DemandForecastEngine demandForecastEngine;

    @Override
    public AdvancedPriceSessionResponse createSession(
        CreateAdvancedPriceSessionRequest request
    ) {
        validateSessionInput(request);

        Long sellerId = requireCurrentUserId();
        ProductInfo product = DssCostSupport.normalizeProductCost(
            productService.getProductInfo(request.getProductId())
        );
        validateOwnership(product, sellerId);

        ElasticitySnapshot elasticity = resolveElasticity(
            product.id(),
            request.getFromDate(),
            request.getToDate()
        );

        DemandForecastComputation forecast = demandForecastEngine.forecast(
            new DemandForecastProductView(
                product.id(),
                product.sellerId(),
                product.name(),
                product.price()
            ),
            request.getFromDate(),
            request.getToDate(),
            request.getForecastPeriod()
        );

        if (forecast.insufficientData()) {
            throw new BusinessException(
                "Không đủ dữ liệu bán hàng để dự báo nhu cầu."
            );
        }

        AdvancedPriceSession session = sessionRepository.save(
            AdvancedPriceSession.builder()
                .sellerId(sellerId)
                .productId(product.id())
                .productName(product.name())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .forecastPeriod(request.getForecastPeriod())
                .estimatedOrderCost(money(request.getEstimatedOrderCost()))
                .basePrice(money(product.price()))
                .costPrice(money(product.costPrice()))
                .historicalQuantitySold(elasticity.quantitySold())
                .averageElasticity(elasticity.value())
                .elasticitySource(elasticity.source())
                .baselineForecastDemand(forecast.predictedDemand())
                .forecastMethod(forecast.method())
                .status(AdvancedPriceSessionStatus.ACTIVE)
                .build()
        );

        log.info(
            "Created advanced price session {} for product {} using {}",
            session.getId(),
            product.id(),
            forecast.method()
        );

        return toSessionResponse(session, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public AdvancedPriceSessionResponse getSession(Long sessionId) {
        Long sellerId = requireCurrentUserId();
        AdvancedPriceSession session = sessionRepository
            .findByIdAndSellerId(sessionId, sellerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Advanced price session",
                sessionId
            ));

        return toSessionResponse(session, loadScenarios(sessionId));
    }

    @Override
    @Transactional
    public AdvancedPriceSessionResponse createScenario(
        Long sessionId,
        CreateAdvancedPriceScenarioRequest request
    ) {
        Long sellerId = requireCurrentUserId();
        AdvancedPriceSession session = findOwnedSessionForUpdate(
            sessionId,
            sellerId
        );
        ensureSessionActive(session);

        BigDecimal changePercent = request.getPriceChangePercent()
            .setScale(2, RoundingMode.HALF_UP);
        if (changePercent.compareTo(MIN_PRICE_CHANGE) < 0
            || changePercent.compareTo(MAX_PRICE_CHANGE) > 0) {
            throw new BusinessException(
                "Phần trăm đổi giá phải nằm trong khoảng từ -70 đến 100."
            );
        }

        if (scenarioRepository.existsBySessionIdAndPriceChangePercent(
            sessionId,
            changePercent
        )) {
            throw new BusinessException(
                "Mỗi kịch bản trong cùng phiên phải có phần trăm đổi giá khác nhau."
            );
        }

        BigDecimal changeRate = changePercent.divide(
            ONE_HUNDRED,
            RATE_SCALE,
            RoundingMode.HALF_UP
        );
        BigDecimal newPrice = money(
            session.getBasePrice().multiply(BigDecimal.ONE.add(changeRate))
        );
        if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Giá mô phỏng phải lớn hơn 0.");
        }

        BigDecimal demandMultiplier = BigDecimal.ONE
            .add(session.getAverageElasticity().multiply(changeRate))
            .max(BigDecimal.ZERO)
            .setScale(RATE_SCALE, RoundingMode.HALF_UP);
        long forecastDemand = BigDecimal
            .valueOf(session.getBaselineForecastDemand())
            .multiply(demandMultiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();
        BigDecimal profitPerProduct = money(
            newPrice
                .subtract(session.getCostPrice())
                .subtract(session.getEstimatedOrderCost())
        );
        BigDecimal expectedProfit = money(
            profitPerProduct.multiply(BigDecimal.valueOf(forecastDemand))
        );

        List<AdvancedPriceScenario> existingScenarios = loadScenarios(sessionId);
        if (existingScenarios.size() >= MAX_SCENARIOS) {
            AdvancedPriceScenario oldest = existingScenarios
                .get(existingScenarios.size() - 1);
            scenarioRepository.delete(oldest);
            scenarioRepository.flush();
        }

        scenarioRepository.save(
            AdvancedPriceScenario.builder()
                .sessionId(sessionId)
                .priceChangePercent(changePercent)
                .newPrice(newPrice)
                .profitPerProduct(profitPerProduct)
                .demandMultiplier(demandMultiplier)
                .forecastDemand(forecastDemand)
                .expectedProfit(expectedProfit)
                .build()
        );

        return toSessionResponse(session, loadScenarios(sessionId));
    }

    @Override
    @Transactional
    public ApplyAdvancedPriceScenarioResponse applyScenario(
        Long sessionId,
        Long scenarioId
    ) {
        Long sellerId = requireCurrentUserId();
        AdvancedPriceSession session = findOwnedSessionForUpdate(
            sessionId,
            sellerId
        );
        ensureSessionActive(session);

        AdvancedPriceScenario scenario = scenarioRepository
            .findByIdAndSessionId(scenarioId, sessionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Advanced price scenario",
                scenarioId
            ));
        ProductInfo product = productService.getProductInfo(session.getProductId());
        validateOwnership(product, sellerId);

        if (product.price().compareTo(session.getBasePrice()) != 0) {
            throw new BusinessException(
                "Giá sản phẩm đã thay đổi sau khi tạo phiên. Hãy tạo phiên phân tích mới."
            );
        }

        UpdateProductRequest updateRequest = new UpdateProductRequest();
        updateRequest.setPrice(scenario.getNewPrice());
        productService.updateProduct(product.id(), updateRequest);

        OffsetDateTime appliedAt = OffsetDateTime.now(ZoneOffset.UTC);
        scenario.setAppliedAt(appliedAt);
        session.setAppliedAt(appliedAt);
        session.setStatus(AdvancedPriceSessionStatus.APPLIED);
        scenarioRepository.save(scenario);
        sessionRepository.save(session);

        log.info(
            "Applied advanced price scenario {} to product {}: {} -> {}",
            scenarioId,
            product.id(),
            session.getBasePrice(),
            scenario.getNewPrice()
        );

        return ApplyAdvancedPriceScenarioResponse.builder()
            .sessionId(sessionId)
            .scenarioId(scenarioId)
            .productId(product.id())
            .oldPrice(session.getBasePrice())
            .newPrice(scenario.getNewPrice())
            .priceChangePercent(scenario.getPriceChangePercent())
            .appliedAt(appliedAt)
            .build();
    }

    private ElasticitySnapshot resolveElasticity(
        Long productId,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        try {
            PriceElasticitySnapshot analysis = priceElasticityService
                .analyze(productId, fromDate, toDate);
            return new ElasticitySnapshot(
                analysis.averageElasticity(),
                analysis.quantitySold(),
                SELECTED_RANGE
            );
        } catch (BusinessException selectedRangeFailure) {
            try {
                PriceElasticitySnapshot fallback = priceElasticityService
                    .analyzeAllHistory(productId);
                long quantitySold = orderService.getCompletedQuantitySold(
                    productId,
                    fromDate,
                    toDate
                );
                if (quantitySold <= 0) {
                    throw selectedRangeFailure;
                }
                return new ElasticitySnapshot(
                    fallback.averageElasticity()
                        .setScale(RATE_SCALE, RoundingMode.HALF_UP),
                    quantitySold,
                    ALL_HISTORY_FALLBACK
                );
            } catch (BusinessException fallbackFailure) {
                throw new BusinessException(
                    "Không đủ biến động giá và dữ liệu bán hàng để tính hệ số co giãn E."
                );
            }
        }
    }

    private void validateSessionInput(CreateAdvancedPriceSessionRequest request) {
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new BusinessException(
                "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc."
            );
        }
        if (request.getToDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Ngày kết thúc không được ở tương lai.");
        }

        long historyDays = ChronoUnit.DAYS.between(
            request.getFromDate(),
            request.getToDate()
        ) + 1L;
        if (historyDays < MIN_HISTORY_DAYS || historyDays > MAX_HISTORY_DAYS) {
            throw new BusinessException(
                "Khoảng dữ liệu lịch sử phải từ 7 đến 180 ngày."
            );
        }
        if (!ALLOWED_FORECAST_PERIODS.contains(request.getForecastPeriod())) {
            throw new BusinessException(
                "Khoảng dự báo chỉ nhận một trong các giá trị 7, 14 hoặc 30 ngày."
            );
        }
    }

    private void validateProductPrices(ProductInfo product) {
        DssCostSupport.normalizeProductCost(product);
    }

    private void validateOwnership(ProductInfo product, Long sellerId) {
        if (!sellerId.equals(product.sellerId())) {
            throw new ForbiddenException(
                "Bạn không có quyền phân tích hoặc cập nhật sản phẩm này."
            );
        }
    }

    private AdvancedPriceSession findOwnedSessionForUpdate(
        Long sessionId,
        Long sellerId
    ) {
        return sessionRepository
            .findOwnedByIdForUpdate(sessionId, sellerId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Advanced price session",
                sessionId
            ));
    }

    private void ensureSessionActive(AdvancedPriceSession session) {
        if (session.getStatus() != AdvancedPriceSessionStatus.ACTIVE) {
            throw new BusinessException(
                "Phiên đã áp dụng giá. Hãy tạo phiên phân tích mới."
            );
        }
    }

    private Long requireCurrentUserId() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required.");
        }
        return currentUserId;
    }

    private List<AdvancedPriceScenario> loadScenarios(Long sessionId) {
        return scenarioRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    private AdvancedPriceSessionResponse toSessionResponse(
        AdvancedPriceSession session,
        List<AdvancedPriceScenario> scenarios
    ) {
        List<AdvancedPriceScenarioResponse> scenarioResponses = scenarios
            .stream()
            .map(scenario -> toScenarioResponse(session, scenario))
            .toList();

        return AdvancedPriceSessionResponse.builder()
            .sessionId(session.getId())
            .status(session.getStatus())
            .productSummary(AdvancedPriceProductSummaryResponse.builder()
                .productId(session.getProductId())
                .productName(session.getProductName())
                .fromDate(session.getFromDate())
                .toDate(session.getToDate())
                .forecastPeriod(session.getForecastPeriod())
                .currentPrice(session.getBasePrice())
                .costPrice(session.getCostPrice())
                .estimatedOrderCost(session.getEstimatedOrderCost())
                .historicalQuantitySold(session.getHistoricalQuantitySold())
                .build())
            .averageElasticity(session.getAverageElasticity())
            .elasticitySource(session.getElasticitySource())
            .baselineForecastDemand(session.getBaselineForecastDemand())
            .forecastMethod(session.getForecastMethod())
            .latestScenario(scenarioResponses.isEmpty()
                ? null
                : scenarioResponses.get(0))
            .scenarios(scenarioResponses)
            .scenarioCount(scenarioResponses.size())
            .maxScenarios(MAX_SCENARIOS)
            .appliedAt(session.getAppliedAt())
            .createdAt(session.getCreatedAt())
            .build();
    }

    private AdvancedPriceScenarioResponse toScenarioResponse(
        AdvancedPriceSession session,
        AdvancedPriceScenario scenario
    ) {
        return AdvancedPriceScenarioResponse.builder()
            .scenarioId(scenario.getId())
            .priceChangePercent(scenario.getPriceChangePercent())
            .costPrice(session.getCostPrice())
            .newPrice(scenario.getNewPrice())
            .profitPerProduct(scenario.getProfitPerProduct())
            .baselineForecastDemand(session.getBaselineForecastDemand())
            .demandMultiplier(scenario.getDemandMultiplier())
            .forecastDemand(scenario.getForecastDemand())
            .expectedProfit(scenario.getExpectedProfit())
            .createdAt(scenario.getCreatedAt())
            .appliedAt(scenario.getAppliedAt())
            .applied(scenario.getAppliedAt() != null)
            .build();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record ElasticitySnapshot(
        BigDecimal value,
        long quantitySold,
        String source
    ) {
    }
}

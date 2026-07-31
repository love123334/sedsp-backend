package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.request.GenerateDemandPredictionRequest;
import com.example.secdsp.modules.dss.dto.response.DemandPredictionResponse;
import com.example.secdsp.modules.dss.entity.DemandPrediction;
import com.example.secdsp.modules.dss.mapper.DemandPredictionMapper;
import com.example.secdsp.modules.dss.repository.DemandPredictionRepository;
import com.example.secdsp.modules.order.service.OrderService;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandPredictionServiceImpl
    implements DemandPredictionService {

    private static final String INSUFFICIENT_DATA_MESSAGE =
        "Không đủ dữ liệu để tạo dự báo.";
    private static final int DEMAND_SCALE = 2;

    private final DemandPredictionRepository demandPredictionRepository;
    private final DemandPredictionMapper demandPredictionMapper;
    private final ProductService productService;
    private final OrderService orderService;

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

        ProductInfo product = productService
            .getProductInfo(request.getProductId());

        Long currentUserId = requireCurrentUserId();
        validateProductAccess(product, currentUserId);

        LocalDate firstSaleDate = orderService
            .getFirstCompletedSaleDate(product.id());

        if (firstSaleDate == null) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate
            .minusDays(request.getHistoricalDays() - 1L);

        if (firstSaleDate.isAfter(startDate)) {
            throw new BusinessException(INSUFFICIENT_DATA_MESSAGE);
        }

        long totalQuantitySold = orderService
            .getCompletedQuantitySold(
                product.id(),
                startDate,
                endDate
            );

        BigDecimal averageDailyDemand = BigDecimal
            .valueOf(totalQuantitySold)
            .divide(
                BigDecimal.valueOf(request.getHistoricalDays()),
                DEMAND_SCALE,
                RoundingMode.HALF_UP
            );

        BigDecimal predictedDemand = averageDailyDemand
            .multiply(BigDecimal.valueOf(request.getForecastPeriod()))
            .setScale(DEMAND_SCALE, RoundingMode.HALF_UP);

        DemandPrediction prediction = DemandPrediction.builder()
            .product(buildProductRef(product.id()))
            .historicalDays(request.getHistoricalDays())
            .forecastPeriod(request.getForecastPeriod())
            .averageDailyDemand(averageDailyDemand)
            .predictedQuantity(predictedDemand)
            .generatedBy(buildUserRef(currentUserId))
            .build();

        DemandPrediction saved = demandPredictionRepository.save(prediction);

        log.info(
            "Demand prediction {} generated successfully for product {}",
            saved.getId(),
            product.id()
        );

        return demandPredictionMapper.toResponse(saved);
    }

    private Long requireCurrentUserId() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        return currentUserId;
    }

    private void validateProductAccess(
        ProductInfo product,
        Long currentUserId
    ) {
        if (SecurityUtils.hasRole(UserRole.SELLER)
            && !currentUserId.equals(product.sellerId())) {
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

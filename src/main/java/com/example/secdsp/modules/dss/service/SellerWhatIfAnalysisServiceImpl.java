package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.dss.dto.request.SellerDiscountAnalysisRequest;
import com.example.secdsp.modules.dss.dto.response.SellerDiscountAnalysisResponse;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerWhatIfAnalysisServiceImpl
    implements SellerWhatIfAnalysisService {

    private final ProductService productService;
    private final DemandPredictionService demandPredictionService;
    private final PricePredictionService pricePredictionService;
    private final SellerDiscountProfitCalculator calculator;

    @Override
    @Transactional(readOnly = true)
    public SellerDiscountAnalysisResponse analyzeDiscount(
        SellerDiscountAnalysisRequest request
    ) {
        ProductInfo product = productService
            .getProductInfo(request.getProductId());

        Long sellerId = requireCurrentUserId();
        validateProductOwnership(product, sellerId);
        validateProductPrices(product);

        log.info(
            "Analyzing {}% discount for product {} over {} days",
            request.getDiscountPercentage(),
            product.id(),
            request.getSimulationPeriod()
        );

        double forecastDemand = demandPredictionService.predictDemand(
            product.id(),
            request.getSimulationPeriod()
        );
        double elasticity = pricePredictionService
            .calculateElasticity(product.id());

        SellerDiscountAnalysisResponse response = calculator.calculate(
            product.price(),
            product.costPrice(),
            request.getDiscountPercentage(),
            forecastDemand,
            elasticity
        );

        log.info(
            "Discount analysis completed for product {} with expected profit {}",
            product.id(),
            response.getExpectedProfit()
        );

        return response;
    }

    private void validateProductPrices(ProductInfo product) {
        if (product.price() == null
            || product.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                "Giá bán sản phẩm phải lớn hơn 0."
            );
        }

        if (product.costPrice() == null
            || product.costPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                "Sản phẩm phải có giá vốn để thực hiện phân tích."
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

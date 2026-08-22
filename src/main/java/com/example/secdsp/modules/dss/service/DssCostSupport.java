package com.example.secdsp.modules.dss.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Chuẩn hóa giá vốn cho phân tích DSS khi seller chưa nhập cost_price. */
public final class DssCostSupport {

    private static final BigDecimal DEFAULT_COST_RATIO = new BigDecimal("0.70");

    private DssCostSupport() {}

    public static BigDecimal effectiveCostPrice(BigDecimal costPrice, BigDecimal salePrice) {
        if (costPrice != null && costPrice.compareTo(BigDecimal.ZERO) > 0) {
            return costPrice.setScale(2, RoundingMode.HALF_UP);
        }
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) {
            return salePrice
                .multiply(DEFAULT_COST_RATIO)
                .setScale(2, RoundingMode.HALF_UP);
        }
        throw new BusinessException(
            "Sản phẩm phải có giá bán hợp lệ để chạy phân tích DSS."
        );
    }

    public static ProductInfo normalizeProductCost(ProductInfo product) {
        if (product.price() == null || product.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Giá bán sản phẩm phải lớn hơn 0.");
        }
        BigDecimal cost = effectiveCostPrice(product.costPrice(), product.price());
        if (product.costPrice() != null && cost.compareTo(product.costPrice()) == 0) {
            return product;
        }
        return new ProductInfo(
            product.id(),
            product.sellerId(),
            product.name(),
            product.price(),
            cost,
            product.status()
        );
    }
}

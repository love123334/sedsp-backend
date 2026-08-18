package com.example.secdsp.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.dss")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DssProperties {

    /** Thư mục model LightGBM ONNX (global-demand.onnx). */
    String modelDir = "models/demand";

    /**
     * When true, missing/unrunnable ONNX fails boot. Railway Docker keeps this
     * false because onnxruntime is compileOnly ({@code -PexcludeOnnx}).
     */
    boolean modelRequired = false;

    /** Kịch bản % thay đổi giá (giảm / giữ / tăng). Có thể override qua env. */
    int defaultForecastDays = 30;

    /** Giới hạn % thay đổi giá trong what-if (±). */
    int maxPriceChangePercent = 300;

    List<Integer> priceChangePercentages = new ArrayList<>(List.of(-10, -5, 0, 5, 10));

    /** % phí nền tảng trên doanh thu (ước tính — không có bảng phí riêng trong DB). */
    BigDecimal platformFeePercent = new BigDecimal("2.0");

    /** Chi phí giao hàng trung bình / đơn vị (VND) — ước tính khi không tách được theo SP. */
    BigDecimal avgShippingPerUnitVnd = new BigDecimal("25000");

    /** Chi phí vận hành cố định / đơn vị (VND) — cấu hình, mặc định 0. */
    BigDecimal operatingCostPerUnitVnd = BigDecimal.ZERO;

    boolean includeShippingInProfit = true;

    boolean includePlatformFee = true;

    /** Ngưỡng % thay đổi LN coi là “ổn định” trong what-if. */
    BigDecimal profitTolerancePercent = new BigDecimal("3.0");
}

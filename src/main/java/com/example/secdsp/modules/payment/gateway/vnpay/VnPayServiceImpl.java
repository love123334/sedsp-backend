package com.example.secdsp.modules.payment.gateway.vnpay;

import com.example.secdsp.common.exception.PaymentGatewayException;
import com.example.secdsp.config.VnPayProperties;
import com.example.secdsp.modules.payment.dto.request.PaymentGatewayRequest;
import com.example.secdsp.modules.payment.dto.response.PaymentGatewayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnPayServiceImpl implements VnPayService {

    private static final ZoneId VNP_TZ = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayProperties properties;

    @Override
    public PaymentGatewayResponse createPayment(PaymentGatewayRequest request) {
        requireConfigured();

        String txnRef = String.valueOf(System.currentTimeMillis());
        ZonedDateTime now = ZonedDateTime.now(VNP_TZ);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", blankToDefault(properties.getVersion(), "2.1.0"));
        params.put("vnp_Command", blankToDefault(properties.getCommand(), "pay"));
        params.put("vnp_TmnCode", properties.getTmnCode().trim());
        params.put(
            "vnp_Amount",
            request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString()
        );
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        // VNPay: ASCII, no accents, no special chars (#, :, etc.)
        params.put("vnp_OrderInfo", sanitizeOrderInfo(request.getOrderInfo(), request.getOrderId()));
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.getReturnUrl().trim());
        params.put("vnp_IpAddr", "127.0.0.1");
        // Required — missing these causes "Du lieu gui sang khong dung dinh dang"
        params.put("vnp_CreateDate", now.format(VNP_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNP_DATE));

        String queryUrl = buildQuery(params);
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(properties.getSecretKey().trim(), hashData);

        String paymentUrl = properties.getPayUrl().trim()
            + "?"
            + queryUrl
            + "&vnp_SecureHash="
            + secureHash;

        log.info("VNPay payment URL created txnRef={} amount={}", txnRef, params.get("vnp_Amount"));

        return PaymentGatewayResponse.builder()
            .redirectUrl(paymentUrl)
            .transactionRef(txnRef)
            .build();
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        Map<String, String> fields = new TreeMap<>(params);
        String receivedHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        String calculatedHash = hmacSHA512(properties.getSecretKey().trim(), buildHashData(fields));
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    private void requireConfigured() {
        if (isBlank(properties.getTmnCode())
            || properties.getTmnCode().startsWith("YOUR_")
            || isBlank(properties.getSecretKey())
            || properties.getSecretKey().startsWith("YOUR_")
            || isBlank(properties.getPayUrl())
            || isBlank(properties.getReturnUrl())) {
            throw new IllegalStateException(
                "VNPay chua cau hinh. Dat VNPAY_TMN_CODE, VNPAY_HASH_SECRET, VNPAY_RETURN_URL tren Railway."
            );
        }
    }

    private static String sanitizeOrderInfo(String orderInfo, Long orderId) {
        String raw = (orderInfo == null || orderInfo.isBlank())
            ? "Thanh toan don hang " + orderId
            : orderInfo;
        // Strip non-alphanumeric except space — VNPay rejects special chars
        String cleaned = raw.replaceAll("[^a-zA-Z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.isEmpty()) {
            cleaned = "Thanh toan don hang " + orderId;
        }
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    /** Match VNPay Java demo: sorted keys, URL-encoded values, skip empties. */
    private static String buildHashData(Map<String, String> params) {
        // Hash uses raw field names + encoded values (official Java demo)
        return joinEncoded(params, false);
    }

    private static String buildQuery(Map<String, String> params) {
        return joinEncoded(params, true);
    }

    private static String joinEncoded(Map<String, String> params, boolean encodeKeys) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(encodeKeys ? encode(fieldName) : fieldName)
                .append('=')
                .append(encode(fieldValue));
        }
        return sb.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new PaymentGatewayException("Error generating VNPay hash", e);
        }
    }

    private static String blankToDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

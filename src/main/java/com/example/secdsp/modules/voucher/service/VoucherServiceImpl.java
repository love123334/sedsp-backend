package com.example.secdsp.modules.voucher.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.modules.cart.entity.Cart;
import com.example.secdsp.modules.cart.entity.CartItem;
import com.example.secdsp.modules.cart.repository.CartRepository;
import com.example.secdsp.modules.order.entity.Order;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.voucher.dto.*;
import com.example.secdsp.modules.voucher.entity.*;
import com.example.secdsp.modules.voucher.repository.VoucherRepository;
import com.example.secdsp.modules.voucher.repository.VoucherRequestRepository;
import com.example.secdsp.modules.voucher.repository.VoucherUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherRequestRepository voucherRequestRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final CartRepository cartRepository;

    @Override
    @Transactional
    public VoucherResponse createManagerVoucher(UpsertVoucherRequest request, Long managerId) {
        validateUpsert(request);
        Voucher voucher = mapNewVoucher(request, managerId, null);
        voucher = voucherRepository.save(voucher);
        return toVoucherResponse(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> listManagerVouchers() {
        return voucherRepository.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(this::toVoucherResponse)
            .toList();
    }

    @Override
    @Transactional
    public VoucherResponse setVoucherActive(Long id, boolean active) {
        Voucher voucher = voucherRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new ResourceNotFoundException("Voucher", id));
        voucher.setIsActive(active);
        return toVoucherResponse(Objects.requireNonNull(voucherRepository.save(voucher)));
    }

    @Override
    @Transactional
    public VoucherRequestResponse createSellerRequest(CreateVoucherRequestDto request, Long sellerId) {
        if (voucherRequestRepository.existsBySeller_IdAndCodeIgnoreCaseAndStatus(
            sellerId, request.getCode().trim(), VoucherRequestStatus.PENDING)) {
            throw new BusinessException("Bạn đã có yêu cầu voucher với mã này đang chờ duyệt.");
        }
        if (request.getEndsAt().isBefore(request.getStartsAt())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        VoucherRequest entity = new VoucherRequest();
        User seller = userRef(sellerId);
        entity.setSeller(seller);
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDiscountType(request.getDiscountType());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setAppliesTo(request.getAppliesTo());
        entity.setMinimumOrderAmount(
            request.getMinimumOrderAmount() != null
                ? request.getMinimumOrderAmount()
                : BigDecimal.ZERO
        );
        entity.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        entity.setUsageLimit(request.getUsageLimit());
        entity.setStartsAt(request.getStartsAt());
        entity.setEndsAt(request.getEndsAt());
        entity.setProducts(resolveSellerProducts(request.getProductIds(), sellerId, request.getAppliesTo()));
        entity = voucherRequestRepository.save(entity);
        return toRequestResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherRequestResponse> listSellerRequests(Long sellerId) {
        return voucherRequestRepository.findBySeller_IdOrderByCreatedAtDesc(sellerId).stream()
            .map(this::toRequestResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherRequestResponse> listPendingRequests() {
        return voucherRequestRepository.findByStatusOrderByCreatedAtDesc(VoucherRequestStatus.PENDING).stream()
            .map(this::toRequestResponse)
            .toList();
    }

    @Override
    @Transactional
    public VoucherRequestResponse approveRequest(
        Long requestId,
        ReviewVoucherRequestDto review,
        Long managerId
    ) {
        VoucherRequest request = voucherRequestRepository.findDetailedById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("VoucherRequest", requestId));
        if (request.getStatus() != VoucherRequestStatus.PENDING) {
            throw new BusinessException("Yêu cầu đã được xử lý.");
        }

        UpsertVoucherRequest upsert = new UpsertVoucherRequest();
        upsert.setCode(request.getCode());
        upsert.setName(request.getName());
        upsert.setDescription(request.getDescription());
        upsert.setDiscountType(request.getDiscountType());
        upsert.setDiscountValue(request.getDiscountValue());
        upsert.setScope(VoucherScope.SHOP);
        upsert.setSellerId(request.getSeller().getId());
        upsert.setAppliesTo(request.getAppliesTo());
        upsert.setMinimumOrderAmount(request.getMinimumOrderAmount());
        upsert.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        upsert.setUsageLimit(request.getUsageLimit());
        upsert.setStartsAt(request.getStartsAt());
        upsert.setEndsAt(request.getEndsAt());
        upsert.setProductIds(productIdsFrom(request.getProducts()));

        Voucher voucher = mapNewVoucher(upsert, managerId, request.getId());
        voucher = voucherRepository.save(voucher);

        request.setStatus(VoucherRequestStatus.APPROVED);
        request.setManagerNote(review != null ? review.getManagerNote() : null);
        request.setReviewedBy(userRef(managerId));
        request.setReviewedAt(OffsetDateTime.now());
        request.setVoucherId(voucher.getId());
        voucherRequestRepository.save(request);

        return toRequestResponse(request);
    }

    @Override
    @Transactional
    public VoucherRequestResponse rejectRequest(
        Long requestId,
        ReviewVoucherRequestDto review,
        Long managerId
    ) {
        VoucherRequest request = voucherRequestRepository.findDetailedById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("VoucherRequest", requestId));
        if (request.getStatus() != VoucherRequestStatus.PENDING) {
            throw new BusinessException("Yêu cầu đã được xử lý.");
        }
        request.setStatus(VoucherRequestStatus.REJECTED);
        request.setManagerNote(review != null ? review.getManagerNote() : "Không đáp ứng điều kiện.");
        request.setReviewedBy(userRef(managerId));
        request.setReviewedAt(OffsetDateTime.now());
        return toRequestResponse(voucherRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateVoucherResponse validateForCart(Long userId, ValidateVoucherRequest request) {
        try {
            List<Long> productIds = resolveProductIds(userId, request);
            return doValidate(request.getCode(), productIds, userId, false);
        } catch (BusinessException ex) {
            return invalid(friendlyVoucherMessage(ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return invalid("Sản phẩm trong giỏ không còn tồn tại.");
        } catch (Exception ex) {
            log.warn("Voucher validate failed for user {}", userId, ex);
            return invalid("Chưa áp dụng được mã giảm giá. Vui lòng thử lại sau.");
        }
    }

    private static String friendlyVoucherMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Mã giảm giá không hợp lệ.";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("product") && lower.contains("not found")) {
            return "Sản phẩm trong giỏ không còn tồn tại.";
        }
        if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("schema")) {
            return "Chưa áp dụng được mã giảm giá. Vui lòng thử lại sau.";
        }
        return raw;
    }

    private List<Long> resolveProductIds(Long userId, ValidateVoucherRequest request) {
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            return request.getProductIds();
        }
        Cart cart = cartRepository.findByUser_Id(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            for (int i = 0; i < item.getQuantity(); i++) {
                ids.add(item.getProduct().getId());
            }
        }
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> listPublicVouchers(Long sellerId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Voucher> list = sellerId != null
            ? voucherRepository.findActiveBySeller(sellerId, now)
            : voucherRepository.findActivePublic(now);
        return list.stream().map(this::toVoucherResponse).toList();
    }

    @Override
    @Transactional
    public AppliedVoucher applyToOrder(
        Order order,
        String voucherCode,
        List<CartItem> cartItems,
        Long userId
    ) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        List<Long> productIds = new ArrayList<>();
        for (CartItem item : cartItems) {
            for (int i = 0; i < item.getQuantity(); i++) {
                productIds.add(item.getProduct().getId());
            }
        }
        ValidateVoucherResponse validated = doValidate(voucherCode, productIds, userId, true);
        if (!validated.valid()) {
            throw new BusinessException(validated.message());
        }
        Voucher voucher = voucherRepository.findById(Objects.requireNonNull(validated.voucherId()))
            .orElseThrow(() -> new BusinessException("Voucher không hợp lệ."));

        order.setDiscountAmount(validated.discountAmount());
        order.setTotalAmount(
            order.getSubtotalAmount()
                .add(order.getShippingFee())
                .subtract(validated.discountAmount())
                .max(BigDecimal.ZERO)
        );

        User user = userRef(userId);
        order.setVoucherCode(voucher.getCode());
        order.setVoucherId(voucher.getId());
        voucher.setUsedCount(voucher.getUsedCount() + 1);

        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(voucher);
        usage.setUser(user);
        usage.setOrder(order);
        voucherUsageRepository.save(usage);

        return new AppliedVoucher(voucher, validated.discountAmount());
    }

    private ValidateVoucherResponse doValidate(
        String rawCode,
        List<Long> productIds,
        Long userId,
        boolean strict
    ) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            return invalid("Vui lòng nhập mã voucher.");
        }
        if (productIds == null || productIds.isEmpty()) {
            return invalid("Giỏ hàng trống — không thể áp dụng voucher.");
        }

        Map<Long, LineItem> lines = buildLines(productIds);
        BigDecimal cartSubtotal = sumSubtotals(lines.values());

        Optional<Voucher> platform = voucherRepository.findPlatformByCodeIgnoreCase(code);
        if (platform.isPresent()) {
            return validateVoucher(platform.get(), lines, cartSubtotal, cartSubtotal);
        }

        Set<Long> sellerIds = new HashSet<>();
        for (LineItem line : lines.values()) {
            sellerIds.add(line.sellerId());
        }
        for (Long sellerId : sellerIds) {
            Optional<Voucher> shop = voucherRepository.findShopByCodeAndSellerId(code, sellerId);
            if (shop.isEmpty()) {
                continue;
            }
            BigDecimal sellerSubtotal = BigDecimal.ZERO;
            for (LineItem line : lines.values()) {
                if (sellerId.equals(line.sellerId())) {
                    sellerSubtotal = sellerSubtotal.add(line.subtotal());
                }
            }
            ValidateVoucherResponse attempt = validateVoucher(
                shop.get(), lines, sellerSubtotal, cartSubtotal
            );
            if (attempt.valid()) {
                return attempt;
            }
            if (strict) {
                return attempt;
            }
        }

        return invalid("Mã voucher không hợp lệ hoặc không áp dụng cho giỏ hàng này.");
    }

    private ValidateVoucherResponse validateVoucher(
        Voucher voucher,
        Map<Long, LineItem> lines,
        BigDecimal eligibleSubtotal,
        BigDecimal cartSubtotal
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            return invalid("Voucher đã bị vô hiệu hóa.");
        }
        if (now.isBefore(voucher.getStartsAt())) {
            return invalid("Voucher chưa có hiệu lực.");
        }
        if (now.isAfter(voucher.getEndsAt())) {
            return invalid("Voucher đã hết hạn.");
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return invalid("Voucher đã hết lượt sử dụng.");
        }
        if (eligibleSubtotal.compareTo(voucher.getMinimumOrderAmount()) < 0) {
            return invalid("Đơn hàng chưa đạt giá trị tối thiểu để dùng voucher.");
        }
        if (voucher.getAppliesTo() == VoucherAppliesTo.SELECTED_PRODUCTS) {
            Set<Long> allowed = voucher.getProducts().stream()
                .map(p -> p.getId())
                .collect(Collectors.toSet());
            boolean any = lines.keySet().stream().anyMatch(allowed::contains);
            if (!any) {
                return invalid("Voucher không áp dụng cho sản phẩm trong giỏ.");
            }
            eligibleSubtotal = lines.entrySet().stream()
                .filter(e -> allowed.contains(e.getKey()))
                .map(e -> e.getValue().subtotal())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        }

        BigDecimal discount = calculateDiscount(voucher, eligibleSubtotal);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return invalid("Không thể áp dụng voucher cho giỏ hàng này.");
        }
        if (discount.compareTo(cartSubtotal) > 0) {
            discount = cartSubtotal;
        }

        String sellerName = voucher.getSeller() != null
            ? Optional.ofNullable(voucher.getSeller().getUsername()).orElse("")
            : null;

        return ValidateVoucherResponse.builder()
            .valid(true)
            .message("Áp dụng voucher thành công.")
            .voucherId(voucher.getId())
            .code(voucher.getCode())
            .name(voucher.getName())
            .description(voucher.getDescription())
            .discountType(voucher.getDiscountType())
            .discountValue(voucher.getDiscountValue())
            .scope(voucher.getScope())
            .sellerId(voucher.getSeller() != null ? voucher.getSeller().getId() : null)
            .sellerName(sellerName)
            .discountAmount(discount)
            .eligibleSubtotal(eligibleSubtotal)
            .build();
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal eligibleSubtotal) {
        BigDecimal discount;
        if (voucher.getDiscountType() == VoucherDiscountType.PERCENTAGE) {
            discount = eligibleSubtotal.multiply(voucher.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaximumDiscountAmount() != null
                && discount.compareTo(voucher.getMaximumDiscountAmount()) > 0) {
                discount = voucher.getMaximumDiscountAmount();
            }
        } else {
            discount = voucher.getDiscountValue();
        }
        if (discount.compareTo(eligibleSubtotal) > 0) {
            discount = eligibleSubtotal;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, LineItem> buildLines(List<Long> productIds) {
        Map<Long, LineItem> map = new HashMap<>();
        Map<Long, Long> counts = new HashMap<>();
        for (Long pid : productIds) {
            counts.merge(pid, 1L, (a, b) -> Long.valueOf(a.longValue() + b.longValue()));
        }
        for (Map.Entry<Long, Long> e : counts.entrySet()) {
            ProductInfo p = productService.getProductInfo(e.getKey());
            BigDecimal subtotal = p.price().multiply(BigDecimal.valueOf(e.getValue()));
            map.put(e.getKey(), new LineItem(p.sellerId(), subtotal));
        }
        return map;
    }

    private record LineItem(Long sellerId, BigDecimal subtotal) {}

    private static BigDecimal sumSubtotals(Collection<LineItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem item : items) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    private static List<Long> productIdsFrom(Collection<Product> products) {
        List<Long> ids = new ArrayList<>(products.size());
        for (Product product : products) {
            ids.add(product.getId());
        }
        return ids;
    }

    private ValidateVoucherResponse invalid(String message) {
        return ValidateVoucherResponse.builder()
            .valid(false)
            .message(message)
            .build();
    }

    @NonNull
    private Voucher mapNewVoucher(UpsertVoucherRequest request, Long createdBy, Long requestId) {
        validateUpsert(request);
        Voucher voucher = new Voucher();
        voucher.setCode(request.getCode());
        voucher.setName(request.getName());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setScope(request.getScope());
        voucher.setAppliesTo(request.getAppliesTo());
        voucher.setMinimumOrderAmount(
            request.getMinimumOrderAmount() != null
                ? request.getMinimumOrderAmount()
                : BigDecimal.ZERO
        );
        voucher.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setStartsAt(request.getStartsAt());
        voucher.setEndsAt(request.getEndsAt());
        voucher.setCreatedBy(userRef(createdBy));
        voucher.setRequestId(requestId);
        if (request.getScope() == VoucherScope.SHOP) {
            if (request.getSellerId() == null) {
                throw new BusinessException("Voucher shop cần sellerId.");
            }
            voucher.setSeller(userRef(request.getSellerId()));
            voucher.setProducts(
                resolveSellerProducts(request.getProductIds(), request.getSellerId(), request.getAppliesTo())
            );
        } else {
            voucher.setProducts(resolveProducts(request.getProductIds(), request.getAppliesTo()));
        }
        return voucher;
    }

    private void validateUpsert(UpsertVoucherRequest request) {
        if (request.getEndsAt().isBefore(request.getStartsAt())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (request.getScope() == VoucherScope.PLATFORM && request.getSellerId() != null) {
            throw new BusinessException("Voucher toàn sàn không gắn seller.");
        }
        if (request.getScope() == VoucherScope.SHOP && request.getSellerId() == null) {
            throw new BusinessException("Voucher shop cần seller.");
        }
        if (request.getDiscountType() == VoucherDiscountType.PERCENTAGE
            && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Giảm giá % không được vượt quá 100.");
        }
    }

    private Set<Product> resolveProducts(List<Long> productIds, VoucherAppliesTo appliesTo) {
        if (appliesTo != VoucherAppliesTo.SELECTED_PRODUCTS) {
            return new HashSet<>();
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException("Chọn ít nhất một sản phẩm áp dụng voucher.");
        }
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != new HashSet<>(productIds).size()) {
            throw new BusinessException("Có sản phẩm không tồn tại.");
        }
        return new HashSet<>(products);
    }

    private Set<Product> resolveSellerProducts(
        List<Long> productIds,
        Long sellerId,
        VoucherAppliesTo appliesTo
    ) {
        if (appliesTo != VoucherAppliesTo.SELECTED_PRODUCTS) {
            return new HashSet<>();
        }
        Set<Product> products = resolveProducts(productIds, appliesTo);
        for (Product p : products) {
            if (p.getSeller() == null || !sellerId.equals(p.getSeller().getId())) {
                throw new BusinessException("Sản phẩm không thuộc shop của bạn.");
            }
        }
        return products;
    }

    private User userRef(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private VoucherResponse toVoucherResponse(Voucher v) {
        return VoucherResponse.builder()
            .id(v.getId())
            .code(v.getCode())
            .name(v.getName())
            .description(v.getDescription())
            .discountType(v.getDiscountType())
            .discountValue(v.getDiscountValue())
            .scope(v.getScope())
            .sellerId(v.getSeller() != null ? v.getSeller().getId() : null)
            .sellerName(v.getSeller() != null ? v.getSeller().getUsername() : null)
            .appliesTo(v.getAppliesTo())
            .minimumOrderAmount(v.getMinimumOrderAmount())
            .maximumDiscountAmount(v.getMaximumDiscountAmount())
            .usageLimit(v.getUsageLimit())
            .usedCount(v.getUsedCount())
            .startsAt(v.getStartsAt())
            .endsAt(v.getEndsAt())
            .isActive(v.getIsActive())
            .productIds(productIdsFrom(v.getProducts()))
            .requestId(v.getRequestId())
            .createdAt(v.getCreatedAt())
            .build();
    }

    private VoucherRequestResponse toRequestResponse(VoucherRequest r) {
        return VoucherRequestResponse.builder()
            .id(r.getId())
            .sellerId(r.getSeller().getId())
            .sellerName(r.getSeller().getUsername())
            .code(r.getCode())
            .name(r.getName())
            .description(r.getDescription())
            .discountType(r.getDiscountType())
            .discountValue(r.getDiscountValue())
            .appliesTo(r.getAppliesTo())
            .minimumOrderAmount(r.getMinimumOrderAmount())
            .maximumDiscountAmount(r.getMaximumDiscountAmount())
            .usageLimit(r.getUsageLimit())
            .startsAt(r.getStartsAt())
            .endsAt(r.getEndsAt())
            .status(r.getStatus())
            .managerNote(r.getManagerNote())
            .voucherId(r.getVoucherId())
            .productIds(productIdsFrom(r.getProducts()))
            .createdAt(r.getCreatedAt())
            .reviewedAt(r.getReviewedAt())
            .build();
    }
}

package com.example.secdsp.modules.inventory.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.inventory.dto.internal.InventorySummaryInfo;
import com.example.secdsp.modules.inventory.dto.request.UpdateInventoryRequest;
import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;
import com.example.secdsp.modules.inventory.entity.Inventory;
import com.example.secdsp.modules.inventory.entity.InventoryLog;
import com.example.secdsp.modules.inventory.mapper.InventoryMapper;
import com.example.secdsp.modules.inventory.repository.InventoryLogRepository;
import com.example.secdsp.modules.inventory.repository.InventoryRepository;
import com.example.secdsp.modules.product.dto.internal.LowStockProductInfo;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductService productService;

    private static final int LOW_STOCK_THRESHOLD = 5;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {

        productService.getProductInfo(productId);

        Inventory inventory = inventoryRepository
            .findByProduct_Id(productId)
            .orElseThrow(() ->
                             new ResourceNotFoundException(
                                 "Inventory for product",
                                 productId
                             ));

        return buildResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoriesByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<Long> uniqueIds = productIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .limit(200)
            .toList();
        if (uniqueIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Inventory> byProduct = inventoryRepository.findByProduct_IdIn(uniqueIds)
            .stream()
            .collect(Collectors.toMap(
                inv -> inv.getProduct().getId(),
                Function.identity(),
                (a, b) -> a,
                LinkedHashMap::new
            ));
        List<InventoryResponse> out = new ArrayList<>(byProduct.size());
        for (Long id : uniqueIds) {
            Inventory inventory = byProduct.get(id);
            if (inventory != null) {
                out.add(buildResponse(inventory));
            }
        }
        return out;
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(
        Long productId,
        UpdateInventoryRequest request
    ) {

        log.info("Updating inventory for product {}", productId);

        ProductInfo product =
            productService.getProductInfo(productId);

        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (!SecurityUtils.hasRole(UserRole.ADMIN)
            && (product.sellerId() == null
            || !product.sellerId().equals(currentUserId))) {

            throw new BusinessException(
                "You do not have permission to manage the inventory for this product."
            );
        }

        Inventory inventory = inventoryRepository
            .findByProduct_IdForUpdate(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory for product", productId));

        int previous = inventory.getAvailableQuantity();
        int adjustment = request.getAdjustmentQuantity();
        int newQuantity = previous + adjustment;

        if (newQuantity < 0) {
            throw new BusinessException(
                "Inventory quantity cannot be negative."
            );
        }

        inventory.setAvailableQuantity(newQuantity);

        InventoryLog logEntry = InventoryLog.builder()
            .product(inventory.getProduct())
            .changeAmount(adjustment)
            .previousQuantity(previous)
            .currentQuantity(newQuantity)
            .reason(request.getReason())
            .updatedBy(buildCurrentUserRef())
            .build();

        inventoryLogRepository.save(logEntry);

        log.info("Inventory updated successfully for product {}", productId);

        return buildResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryInfo getInventorySummary(Long sellerId) {

        long lowStock =
            inventoryRepository
                .countLowStockBySeller(sellerId);

        long outOfStock =
            inventoryRepository
                .countOutOfStockBySeller(sellerId);

        return InventorySummaryInfo.builder()
            .lowStockProducts(lowStock)
            .outOfStockProducts(outOfStock)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockProductInfo> getLowStockProducts(Long sellerId) {

        return inventoryRepository
            .findLowStockProductsBySeller(sellerId)
            .stream()
            .map(i -> LowStockProductInfo.builder()
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .quantity(i.getAvailableQuantity())
                .build()
            )
            .toList();
    }

    private InventoryResponse buildResponse(Inventory inventory) {

        InventoryResponse base =
            inventoryMapper.toResponse(inventory);

        int currentStock = inventory.getAvailableQuantity()
            + inventory.getReservedQuantity();

        return InventoryResponse.builder()
            .productId(base.getProductId())
            .productName(base.getProductName())
            .availableQuantity(inventory.getAvailableQuantity())
            .reservedQuantity(inventory.getReservedQuantity())
            .currentStock(currentStock)
            .inventoryStatus(calculateStatus(
                inventory.getAvailableQuantity()))
            .build();
    }

    private String calculateStatus(int available) {

        if (available <= 0) {
            return "OUT_OF_STOCK";
        }

        if (available <= LOW_STOCK_THRESHOLD) {
            return "LOW_STOCK";
        }

        return "IN_STOCK";
    }

    private User buildCurrentUserRef() {

        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new BusinessException("Authentication required.");
        }

        User user = new User();
        user.setId(currentUserId);
        return user;
    }
}
package com.example.secdsp.modules.ai.tool;

import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.inventory.dto.internal.InventorySummaryInfo;
import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;
import com.example.secdsp.modules.inventory.service.InventoryService;
import com.example.secdsp.modules.product.dto.internal.LowStockProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryAiTool {

    private final InventoryService inventoryService;

    public InventoryResponse getInventory(Long productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    public InventorySummaryInfo getInventorySummary() {

        Long sellerId = SecurityUtils.getCurrentUserId();

        if (sellerId == null) {
            throw new IllegalStateException(
                "Authenticated seller is required."
            );
        }

        return inventoryService.getInventorySummary(sellerId);
    }

    public List<LowStockProductInfo> getLowStockProducts() {

        Long sellerId = SecurityUtils.getCurrentUserId();

        if (sellerId == null) {
            throw new IllegalStateException(
                "Authenticated seller is required."
            );
        }

        return inventoryService.getLowStockProducts(sellerId);
    }
}
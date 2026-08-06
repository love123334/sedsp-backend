package com.example.secdsp.modules.inventory.service;

import com.example.secdsp.modules.inventory.dto.internal.InventorySummaryInfo;
import com.example.secdsp.modules.inventory.dto.request.UpdateInventoryRequest;
import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;
import com.example.secdsp.modules.product.dto.internal.LowStockProductInfo;

import java.util.List;

public interface InventoryService {

    InventoryResponse getInventoryByProductId(Long productId);

    List<InventoryResponse> getInventoriesByProductIds(List<Long> productIds);

    InventoryResponse updateInventory(
        Long productId,
        UpdateInventoryRequest request
    );

    InventorySummaryInfo getInventorySummary(Long sellerId);

    List<LowStockProductInfo> getLowStockProducts(Long sellerId);
}
package com.example.secdsp.modules.inventory.service;

import com.example.secdsp.modules.inventory.dto.request.UpdateInventoryRequest;
import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;

public interface InventoryService {

    InventoryResponse getInventoryByProductId(Long productId);

    InventoryResponse updateInventory(
        Long productId,
        UpdateInventoryRequest request
    );
}
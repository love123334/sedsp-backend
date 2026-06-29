package com.example.secdsp.modules.inventory.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.inventory.dto.request.UpdateInventoryRequest;
import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;
import com.example.secdsp.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>>
    getInventory(@PathVariable Long productId) {

        return ResponseEntity.ok(
            ApiResponse.success(
                inventoryService.getInventoryByProductId(productId)
            )
        );
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>>
    updateInventory(
        @PathVariable Long productId,
        @Valid @RequestBody UpdateInventoryRequest request
    ) {

        return ResponseEntity.ok(
            ApiResponse.success(
                "Inventory updated successfully",
                inventoryService.updateInventory(productId, request)
            )
        );
    }
}
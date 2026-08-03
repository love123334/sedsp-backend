package com.example.secdsp.modules.inventory.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.inventory.dto.request.UpdateInventoryRequest;
import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;
import com.example.secdsp.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<BaseResponse<InventoryResponse>> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(
            BaseResponse.success(
                inventoryService.getInventoryByProductId(productId)
            )
        );
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<BaseResponse<InventoryResponse>> updateInventory(
        @PathVariable Long productId,
        @Valid @RequestBody UpdateInventoryRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                "Inventory updated successfully",
                inventoryService.updateInventory(productId, request)
            )
        );
    }
}
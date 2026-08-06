package com.example.secdsp.modules.inventory.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.inventory.dto.request.UpdateInventoryRequest;
import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;
import com.example.secdsp.modules.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(
    name = "Inventory Management",
    description = "APIs for viewing and managing product inventory"
)
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<InventoryResponse>>> getInventories(
        @Parameter(description = "Product identifiers (comma-separated)", example = "101,102")
        @RequestParam List<Long> productIds
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                inventoryService.getInventoriesByProductIds(productIds)
            )
        );
    }

    @Operation(
        summary = "Get inventory by product",
        description = "Retrieve inventory information for a specific product."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product or inventory not found", content = @Content)
    })
    @GetMapping("/{productId}")
    public ResponseEntity<BaseResponse<InventoryResponse>> getInventory(

        @Parameter(
            description = "Product identifier",
            example = "101"
        )
        @PathVariable Long productId
    ) {

        return ResponseEntity.ok(
            BaseResponse.success(
                inventoryService.getInventoryByProductId(productId)
            )
        );
    }

    @Operation(
        summary = "Update inventory",
        description = """
            Update product inventory.
            
            Required roles:
            - ADMIN
            - SELLER
            """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product or inventory not found", content = @Content)
    })
    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<BaseResponse<InventoryResponse>> updateInventory(

        @Parameter(
            description = "Product identifier",
            example = "101"
        )
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
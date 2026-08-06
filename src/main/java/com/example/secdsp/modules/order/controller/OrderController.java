package com.example.secdsp.modules.order.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.order.dto.request.CreateOrderRequest;
import com.example.secdsp.modules.order.dto.request.UpdateOrderStatusRequest;
import com.example.secdsp.modules.order.dto.response.OrderDetailResponse;
import com.example.secdsp.modules.order.dto.response.OrderResponse;
import com.example.secdsp.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(
    name = "Order Management",
    description = "APIs for creating and managing customer orders"
)
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    private final OrderService orderService;

    @Operation(
        summary = "Create order",
        description = """
            Create a new order from the current user's shopping cart.
            
            Authentication required.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Cart or product not found", content = @Content)
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<OrderResponse>> createOrder(
        @Valid @RequestBody CreateOrderRequest request
    ) {

        return ResponseEntity.ok(
            BaseResponse.success(
                "Order created successfully",
                orderService.createOrder(request)
            )
        );
    }

    @Operation(
        summary = "Get my orders",
        description = """
            Retrieve the authenticated user's orders with pagination.
            
            Authentication required.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Page<OrderResponse>>> getMyOrders(

        @ParameterObject
        @PageableDefault(
            size = 10,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        )
        Pageable pageable
    ) {

        return ResponseEntity.ok(
            BaseResponse.success(
                orderService.getMyOrders(pageable)
            )
        );
    }

    @GetMapping("/seller")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN','MANAGER')")
    public ResponseEntity<BaseResponse<Page<OrderResponse>>>
    getSellerOrders(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                orderService.getSellerOrders(pageable)
            )
        );
    }

    @Operation(
        summary = "Get order details",
        description = """
            Retrieve detailed information about one of the authenticated user's orders.
            
            Authentication required.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<OrderDetailResponse>> getOrderById(

        @Parameter(
            description = "Order identifier",
            example = "1001"
        )
        @PathVariable Long id
    ) {

        return ResponseEntity.ok(
            BaseResponse.success(
                orderService.getOrderById(id)
            )
        );
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN','MANAGER')")
    public ResponseEntity<BaseResponse<OrderResponse>>
    updateOrderStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(
            BaseResponse.success(
                "Order status updated",
                orderService.updateOrderStatus(id, request)
            )
        );
    }

    @Operation(
        summary = "Cancel order",
        description = """
            Cancel one of the authenticated user's orders.
            
            Only orders that have not been shipped can be cancelled.
            Authentication required.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Order cannot be cancelled", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> cancelOrder(

        @Parameter(
            description = "Order identifier",
            example = "1001"
        )
        @PathVariable Long id
    ) {

        orderService.cancelOrder(id);

        return ResponseEntity.ok(
            BaseResponse.success("Order cancelled successfully")
        );
    }
}
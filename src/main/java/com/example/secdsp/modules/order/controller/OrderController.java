package com.example.secdsp.modules.order.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.order.dto.request.CreateOrderRequest;
import com.example.secdsp.modules.order.dto.response.OrderDetailResponse;
import com.example.secdsp.modules.order.dto.response.OrderResponse;
import com.example.secdsp.modules.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>>
    createOrder(@Valid @RequestBody CreateOrderRequest request) {

        return ResponseEntity.ok(
            ApiResponse.success(
                "Order created successfully",
                orderService.createOrder(request)
            )
        );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>>
    getMyOrders(
        @PageableDefault(size = 10) Pageable pageable
    ) {

        return ResponseEntity.ok(
            ApiResponse.success(
                orderService.getMyOrders(pageable)
            )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderDetailResponse>>
    getOrderById(@PathVariable Long id) {

        return ResponseEntity.ok(
            ApiResponse.success(
                orderService.getOrderById(id)
            )
        );
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>>
    cancelOrder(@PathVariable Long id) {

        orderService.cancelOrder(id);

        return ResponseEntity.ok(
            ApiResponse.success("Order cancelled successfully")
        );
    }
}
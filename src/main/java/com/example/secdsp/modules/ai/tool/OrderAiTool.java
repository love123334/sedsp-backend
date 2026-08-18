package com.example.secdsp.modules.ai.tool;

import com.example.secdsp.modules.order.dto.response.OrderDetailResponse;
import com.example.secdsp.modules.order.dto.response.OrderResponse;
import com.example.secdsp.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderAiTool {

    private final OrderService orderService;

    public Page<OrderResponse> getMyOrders() {
        return orderService.getMyOrders(
            PageRequest.of(0, 10)
        );
    }

    public OrderDetailResponse getOrderDetail(Long orderId) {
        return orderService.getOrderById(orderId);
    }
}
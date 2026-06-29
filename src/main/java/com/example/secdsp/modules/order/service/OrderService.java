package com.example.secdsp.modules.order.service;

import com.example.secdsp.modules.order.dto.request.CreateOrderRequest;
import com.example.secdsp.modules.order.dto.response.OrderDetailResponse;
import com.example.secdsp.modules.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderDetailResponse getOrderById(Long id);

    Page<OrderResponse> getMyOrders(Pageable pageable);

    void cancelOrder(Long id);
}
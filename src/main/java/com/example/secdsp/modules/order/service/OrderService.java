package com.example.secdsp.modules.order.service;

import com.example.secdsp.modules.order.dto.internal.OrderDashboardInfo;
import com.example.secdsp.modules.order.dto.internal.RecentOrderInfo;
import com.example.secdsp.modules.order.dto.internal.TopProductSalesInfo;
import com.example.secdsp.modules.order.dto.request.CreateOrderRequest;
import com.example.secdsp.modules.order.dto.request.UpdateOrderStatusRequest;
import com.example.secdsp.modules.order.dto.response.OrderDetailResponse;
import com.example.secdsp.modules.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderDetailResponse getOrderById(Long id);

    Page<OrderResponse> getMyOrders(Pageable pageable);

    Page<OrderResponse> getSellerOrders(Pageable pageable);

    void cancelOrder(Long id);

    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request);

    OrderDashboardInfo getSellerOrderSummary(Long sellerId);

    List<RecentOrderInfo> getRecentOrders(Long sellerId);

    List<TopProductSalesInfo> getTopSellingProducts(Long sellerId);

    LocalDate getFirstCompletedSaleDate(Long productId);

    long getCompletedQuantitySold(
        Long productId,
        LocalDate startDate,
        LocalDate endDate
    );
}
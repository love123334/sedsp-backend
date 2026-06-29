package com.example.secdsp.modules.order.repository;

import com.example.secdsp.modules.order.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderTrackingRepository
    extends JpaRepository<OrderTracking, Long> {

    List<OrderTracking> findByOrder_IdOrderByCreatedAtDesc(Long orderId);
}
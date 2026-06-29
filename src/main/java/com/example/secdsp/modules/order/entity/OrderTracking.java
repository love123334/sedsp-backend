package com.example.secdsp.modules.order.entity;

import com.example.secdsp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_tracking")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false)
    OrderTrackingEvent event;

    @Column(columnDefinition = "TEXT")
    String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    User updatedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    LocalDateTime createdAt;
}
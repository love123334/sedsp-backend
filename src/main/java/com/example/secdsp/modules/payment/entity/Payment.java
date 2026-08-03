package com.example.secdsp.modules.payment.entity;

import com.example.secdsp.modules.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    Order order;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
        name = "payment_method",
        nullable = false,
        columnDefinition = "payment_method_enum"
    )
    PaymentMethod paymentMethod;

    @Column(name = "gateway_name", length = 50)
    String gatewayName;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "payment_status"
    )
    PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_id")
    String transactionId;

    @Column(nullable = false, length = 10)
    String currency = "VND";

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    String gatewayResponse;

    @Column(name = "paid_at")
    OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
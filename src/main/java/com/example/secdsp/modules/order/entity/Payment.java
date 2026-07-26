package com.example.secdsp.modules.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @Column(name = "payment_method", nullable = false, columnDefinition = "payment_method_enum")
    PaymentMethod paymentMethod;

    @Column(nullable = false)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_status")
    PaymentStatus status;

    @Column(name = "transaction_id")
    String transactionId;

    @Column(nullable = false)
    String currency;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    String gatewayResponse;

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    LocalDateTime createdAt;
}

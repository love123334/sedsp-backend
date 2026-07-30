package com.example.secdsp.modules.payment.repository;

import com.example.secdsp.modules.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository
    extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder_Id(Long orderId);

    Page<Payment> findByOrder_User_Id(
        Long userId,
        Pageable pageable
    );

    boolean existsByTransactionId(String transactionId);

    Optional<Payment> findByTransactionId(String transactionId);
}
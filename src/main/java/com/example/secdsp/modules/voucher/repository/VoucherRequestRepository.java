package com.example.secdsp.modules.voucher.repository;

import com.example.secdsp.modules.voucher.entity.VoucherRequest;
import com.example.secdsp.modules.voucher.entity.VoucherRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoucherRequestRepository extends JpaRepository<VoucherRequest, Long> {

    List<VoucherRequest> findBySeller_IdOrderByCreatedAtDesc(Long sellerId);

    List<VoucherRequest> findByStatusOrderByCreatedAtDesc(VoucherRequestStatus status);

    @Query("""
        select r from VoucherRequest r
        left join fetch r.products
        left join fetch r.seller
        where r.id = :id
        """)
    Optional<VoucherRequest> findDetailedById(@Param("id") Long id);

    boolean existsBySeller_IdAndCodeIgnoreCaseAndStatus(
        Long sellerId,
        String code,
        VoucherRequestStatus status
    );
}

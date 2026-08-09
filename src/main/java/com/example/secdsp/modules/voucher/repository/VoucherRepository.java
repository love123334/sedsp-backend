package com.example.secdsp.modules.voucher.repository;

import com.example.secdsp.modules.voucher.entity.Voucher;
import com.example.secdsp.modules.voucher.entity.VoucherScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    @Query("""
        select v from Voucher v
        left join fetch v.products
        where upper(v.code) = upper(:code)
          and v.scope = com.example.secdsp.modules.voucher.entity.VoucherScope.PLATFORM
        """)
    Optional<Voucher> findPlatformByCodeIgnoreCase(@Param("code") String code);

    @Query("""
        select v from Voucher v
        left join fetch v.products
        where upper(v.code) = upper(:code)
          and v.scope = com.example.secdsp.modules.voucher.entity.VoucherScope.SHOP
          and v.seller.id = :sellerId
        """)
    Optional<Voucher> findShopByCodeAndSellerId(
        @Param("code") String code,
        @Param("sellerId") Long sellerId
    );

    List<Voucher> findByScopeOrderByCreatedAtDesc(VoucherScope scope);

    List<Voucher> findBySeller_IdOrderByCreatedAtDesc(Long sellerId);

    @Query("""
        select v from Voucher v
        left join fetch v.seller
        where v.isActive = true
          and v.startsAt <= :now
          and v.endsAt >= :now
          and (v.usageLimit is null or v.usedCount < v.usageLimit)
        order by v.createdAt desc
        """)
    List<Voucher> findActivePublic(@Param("now") OffsetDateTime now);

    @Query("""
        select v from Voucher v
        left join fetch v.seller
        where v.isActive = true
          and v.startsAt <= :now
          and v.endsAt >= :now
          and (v.usageLimit is null or v.usedCount < v.usageLimit)
          and v.scope = com.example.secdsp.modules.voucher.entity.VoucherScope.SHOP
          and v.seller.id = :sellerId
        order by v.createdAt desc
        """)
    List<Voucher> findActiveBySeller(
        @Param("sellerId") Long sellerId,
        @Param("now") OffsetDateTime now
    );
}

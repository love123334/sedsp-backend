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
        where upper(v.code) = upper(:code)
          and v.scope = com.example.secdsp.modules.voucher.entity.VoucherScope.PLATFORM
        order by v.id asc
        """)
    List<Voucher> findPlatformVouchersByCodeIgnoreCase(@Param("code") String code);

    default Optional<Voucher> findPlatformByCodeIgnoreCase(String code) {
        List<Voucher> matches = findPlatformVouchersByCodeIgnoreCase(code);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    @Query("""
        select v from Voucher v
        where upper(v.code) = upper(:code)
          and v.scope = com.example.secdsp.modules.voucher.entity.VoucherScope.SHOP
          and v.seller.id = :sellerId
        order by v.id asc
        """)
    List<Voucher> findShopVouchersByCodeAndSellerId(
        @Param("code") String code,
        @Param("sellerId") Long sellerId
    );

    default Optional<Voucher> findShopByCodeAndSellerId(String code, Long sellerId) {
        List<Voucher> matches = findShopVouchersByCodeAndSellerId(code, sellerId);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    @Query(value = """
        select vp.product_id
        from voucher_products vp
        where vp.voucher_id = :voucherId
        """, nativeQuery = true)
    List<Long> findLinkedProductIds(@Param("voucherId") Long voucherId);

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

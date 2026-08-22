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
          and v.scope = :scope
        order by v.id asc
        """)
    List<Voucher> findVouchersByCodeAndScope(
        @Param("code") String code,
        @Param("scope") VoucherScope scope
    );

    default Optional<Voucher> findPlatformByCodeIgnoreCase(String code) {
        List<Voucher> matches = findVouchersByCodeAndScope(code, VoucherScope.PLATFORM);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    @Query("""
        select v from Voucher v
        where upper(v.code) = upper(:code)
          and v.scope = :scope
          and v.seller.id = :sellerId
        order by v.id asc
        """)
    List<Voucher> findShopVouchersByCodeSellerAndScope(
        @Param("code") String code,
        @Param("sellerId") Long sellerId,
        @Param("scope") VoucherScope scope
    );

    default Optional<Voucher> findShopByCodeAndSellerId(String code, Long sellerId) {
        List<Voucher> matches = findShopVouchersByCodeSellerAndScope(
            code,
            sellerId,
            VoucherScope.SHOP
        );
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
          and v.scope = :scope
          and v.seller.id = :sellerId
        order by v.createdAt desc
        """)
    List<Voucher> findActiveBySeller(
        @Param("sellerId") Long sellerId,
        @Param("now") OffsetDateTime now,
        @Param("scope") VoucherScope scope
    );

    default List<Voucher> findActiveShopBySeller(Long sellerId, OffsetDateTime now) {
        return findActiveBySeller(sellerId, now, VoucherScope.SHOP);
    }
}

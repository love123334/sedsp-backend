package com.example.secdsp.modules.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductCatalogQueryRepository {

    Page<Long> searchProductIds(
        String keyword,
        Long categoryId,
        Long sellerId,
        String sort,
        Pageable pageable
    );
}

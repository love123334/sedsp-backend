package com.example.secdsp.modules.product.repository;

import com.example.secdsp.modules.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}

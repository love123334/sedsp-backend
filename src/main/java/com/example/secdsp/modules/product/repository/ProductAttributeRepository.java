package com.example.secdsp.modules.product.repository;

import com.example.secdsp.modules.product.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
}

package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.Category;
import com.shopeasy.auth.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySku(String sku);
    long countByCategory(Category category);
    long countByActiveTrue();
    long countByStockAndActiveTrue(int stock);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock > 0 AND p.stock <= p.lowStockThreshold")
    Page<Product> findLowStockProducts(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stock > 0 AND p.stock <= p.lowStockThreshold")
    long countLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock <= 0")
    Page<Product> findOutOfStockProducts(Pageable pageable);
}

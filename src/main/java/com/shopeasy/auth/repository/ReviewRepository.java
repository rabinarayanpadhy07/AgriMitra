package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.Product;
import com.shopeasy.auth.entity.Review;
import com.shopeasy.auth.entity.ReviewStatus;
import com.shopeasy.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    Page<Review> findByProductAndStatusOrderByCreatedAtDesc(Product product, ReviewStatus status, Pageable pageable);
    boolean existsByUserAndProduct(User user, Product product);
    long countByStatus(ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product AND r.status = 'APPROVED'")
    Double averageRatingForProduct(@Param("product") Product product);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product = :product AND r.status = 'APPROVED'")
    long countApprovedForProduct(@Param("product") Product product);
}

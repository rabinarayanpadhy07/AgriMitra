package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.Cart;
import com.shopeasy.auth.entity.CartItem;
import com.shopeasy.auth.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product p LEFT JOIN FETCH p.category WHERE ci.cart = :cart ORDER BY ci.createdAt DESC")
    List<CartItem> findByCartOrderByCreatedAtDesc(@Param("cart") Cart cart);

    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    Optional<CartItem> findByIdAndCart(Long id, Cart cart);
    void deleteByCart(Cart cart);
}

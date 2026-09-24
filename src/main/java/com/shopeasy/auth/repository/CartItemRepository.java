package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.Cart;
import com.shopeasy.auth.entity.CartItem;
import com.shopeasy.auth.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartOrderByCreatedAtDesc(Cart cart);
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    Optional<CartItem> findByIdAndCart(Long id, Cart cart);
    void deleteByCart(Cart cart);
}

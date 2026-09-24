package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.WishlistItemResponse;
import com.shopeasy.auth.entity.Product;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.entity.WishlistItem;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.ProductRepository;
import com.shopeasy.auth.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> list(User user) {
        return wishlistItemRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<WishlistItemResponse> addItem(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!wishlistItemRepository.existsByUserAndProduct(user, product)) {
            wishlistItemRepository.save(WishlistItem.builder().user(user).product(product).build());
        }

        return list(user);
    }

    @Transactional
    public List<WishlistItemResponse> removeItem(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        wishlistItemRepository.findByUserAndProduct(user, product)
                .ifPresent(wishlistItemRepository::delete);

        return list(user);
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Product p = item.getProduct();
        return WishlistItemResponse.builder()
                .id(item.getId())
                .productId(p.getId())
                .productName(p.getName())
                .productImage(p.getImageUrls() != null && !p.getImageUrls().isEmpty() ? p.getImageUrls().get(0) : null)
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .inStock(p.getStock() > 0 && p.getActive())
                .addedAt(item.getCreatedAt())
                .build();
    }
}

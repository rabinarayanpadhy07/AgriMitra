package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.CartResponse;
import com.shopeasy.auth.entity.Cart;
import com.shopeasy.auth.entity.CartItem;
import com.shopeasy.auth.entity.Product;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.CartItemRepository;
import com.shopeasy.auth.repository.CartRepository;
import com.shopeasy.auth.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CartResponse getCart(User user) {
        return toResponse(getOrCreateCart(user));
    }

    @Transactional
    public CartResponse addItem(User user, Long productId, int quantity) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getActive()) {
            throw new ApiException("This product is not currently available");
        }

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
        int newQuantity = (item != null ? item.getQuantity() : 0) + quantity;
        if (newQuantity > product.getStock()) {
            throw new ApiException("Only " + product.getStock() + " unit(s) of " + product.getName() + " available");
        }

        if (item != null) {
            item.setQuantity(newQuantity);
        } else {
            item = CartItem.builder().cart(cart).product(product).quantity(newQuantity).build();
        }
        cartItemRepository.save(item);

        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(User user, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByIdAndCart(itemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            if (quantity > item.getProduct().getStock()) {
                throw new ApiException("Only " + item.getProduct().getStock() + " unit(s) available");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByIdAndCart(itemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCart(cart);
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartOrderByCreatedAtDesc(cart);

        List<CartResponse.CartItemResponse> itemResponses = items.stream().map(i -> {
            Product p = i.getProduct();
            BigDecimal unitPrice = p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getPrice();
            return CartResponse.CartItemResponse.builder()
                    .id(i.getId())
                    .productId(p.getId())
                    .productName(p.getName())
                    .productImage(p.getImageUrls() != null && !p.getImageUrls().isEmpty() ? p.getImageUrls().get(0) : null)
                    .unitPrice(unitPrice)
                    .quantity(i.getQuantity())
                    .lineTotal(unitPrice.multiply(BigDecimal.valueOf(i.getQuantity())))
                    .availableStock(p.getStock())
                    .build();
        }).collect(Collectors.toList());

        BigDecimal subtotal = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(itemResponses)
                .subtotal(subtotal)
                .itemCount(itemResponses.size())
                .build();
    }
}

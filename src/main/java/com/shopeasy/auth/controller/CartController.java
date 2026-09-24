package com.shopeasy.auth.controller;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.CartItemRequest;
import com.shopeasy.auth.dto.CartResponse;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.service.CartService;
import com.shopeasy.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cartService.getCart(user)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartService.addItem(user, request.getProductId(), quantity)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @RequestBody CartItemRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        int quantity = request.getQuantity() != null ? request.getQuantity() : 0;
        return ResponseEntity.ok(ApiResponse.success("Cart updated", cartService.updateItemQuantity(user, itemId, quantity)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartService.removeItem(user, itemId)));
    }
}

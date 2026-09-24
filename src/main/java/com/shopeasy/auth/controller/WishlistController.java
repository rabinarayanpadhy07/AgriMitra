package com.shopeasy.auth.controller;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.WishlistItemResponse;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.service.UserService;
import com.shopeasy.auth.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> list(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved successfully", wishlistService.list(user)));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> add(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long productId) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", wishlistService.addItem(user, productId)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> remove(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long productId) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", wishlistService.removeItem(user, productId)));
    }
}

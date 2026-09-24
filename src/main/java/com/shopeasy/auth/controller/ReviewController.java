package com.shopeasy.auth.controller;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.ReviewRequest;
import com.shopeasy.auth.dto.ReviewResponse;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.service.ReviewService;
import com.shopeasy.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody ReviewRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Review submitted for moderation", reviewService.create(user, request)));
    }
}

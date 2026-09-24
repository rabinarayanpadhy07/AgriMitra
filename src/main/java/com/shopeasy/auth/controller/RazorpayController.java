package com.shopeasy.auth.controller;

import com.shopeasy.auth.dto.*;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.service.RazorpayService;
import com.shopeasy.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/razorpay")
@RequiredArgsConstructor
public class RazorpayController {

    private final RazorpayService razorpayService;
    private final UserService userService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success("Razorpay config retrieved", Map.of("keyId", razorpayService.getKeyId())));
    }

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RazorpayOrderRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        RazorpayOrderResponse response = razorpayService.createRazorpayOrder(user, request.getOrderId());
        return ResponseEntity.ok(ApiResponse.success("Razorpay order created", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OrderResponse>> verifyPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RazorpayVerificationRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        OrderResponse response = razorpayService.verifyPayment(user, request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", response));
    }
}

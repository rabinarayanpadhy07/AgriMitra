package com.shopeasy.auth.controller;

import com.shopeasy.auth.dto.*;
import com.shopeasy.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping({"/api/auth/register", "/register"})
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse userProfile = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userProfile));
    }

    @PostMapping({"/api/auth/login", "/login"})
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping({"/api/auth/logout", "/logout"})
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Logout successful. Session has been invalidated"));
    }

    @PostMapping({"/api/auth/forgot-password", "/forgot-password"})
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String fallbackOtp = authService.forgotPassword(request);
        if (fallbackOtp != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    "OTP generated successfully. (Note: Email SMTP not configured on server; your OTP is: " + fallbackOtp + ")",
                    java.util.Collections.singletonMap("otp", fallbackOtp)
            ));
        }
        return ResponseEntity.ok(ApiResponse.success("If an account exists with this email or mobile, an OTP has been sent."));
    }

    @PostMapping({"/api/auth/verify-otp", "/verify-otp"})
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully. You may now reset your password"));
    }

    @PostMapping({"/api/auth/reset-password", "/reset-password"})
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Previous sessions invalidated. Please log in with your new password"));
    }

    @PutMapping({"/api/auth/change-password", "/change-password"})
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}

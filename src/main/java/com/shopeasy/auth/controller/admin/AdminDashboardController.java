package com.shopeasy.auth.controller.admin;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.admin.AdminDashboardStatsResponse;
import com.shopeasy.auth.dto.admin.AdminUserSummaryResponse;
import com.shopeasy.auth.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", adminDashboardService.getStats()));
    }

    @GetMapping("/recent-users")
    public ResponseEntity<ApiResponse<List<AdminUserSummaryResponse>>> getRecentUsers() {
        return ResponseEntity.ok(ApiResponse.success("Recent users retrieved successfully", adminDashboardService.getRecentUsers()));
    }

    @GetMapping("/recent-seller-applications")
    public ResponseEntity<ApiResponse<List<AdminUserSummaryResponse>>> getRecentSellerApplications() {
        return ResponseEntity.ok(ApiResponse.success("Recent seller applications retrieved successfully", adminDashboardService.getRecentSellerApplications()));
    }
}

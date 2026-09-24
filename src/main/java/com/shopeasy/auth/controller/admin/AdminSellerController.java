package com.shopeasy.auth.controller.admin;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.admin.AdminUserDetailResponse;
import com.shopeasy.auth.dto.admin.AdminUserSummaryResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.SellerStatus;
import com.shopeasy.auth.service.admin.AdminSellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sellers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSellerController {

    private final AdminSellerService adminSellerService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserSummaryResponse>>> listSellers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SellerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "sellerAppliedAt"));
        PageResponse<AdminUserSummaryResponse> result = adminSellerService.listSellers(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Sellers retrieved successfully", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Seller retrieved successfully", adminSellerService.getSellerById(id)));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Seller approved successfully", adminSellerService.approve(id)));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Seller application rejected", adminSellerService.reject(id)));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> suspend(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Seller suspended successfully", adminSellerService.suspend(id)));
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Seller reactivated successfully", adminSellerService.reactivate(id)));
    }
}

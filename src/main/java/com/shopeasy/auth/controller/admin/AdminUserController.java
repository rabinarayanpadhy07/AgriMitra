package com.shopeasy.auth.controller.admin;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.admin.AdminUserDetailResponse;
import com.shopeasy.auth.dto.admin.AdminUserSummaryResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.AccountStatus;
import com.shopeasy.auth.entity.UserRole;
import com.shopeasy.auth.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserSummaryResponse>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<AdminUserSummaryResponse> result = adminUserService.listUsers(search, role, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", adminUserService.getUserById(id)));
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> blockUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", adminUserService.blockUser(id)));
    }

    @PutMapping("/{id}/unblock")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> unblockUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", adminUserService.unblockUser(id)));
    }
}

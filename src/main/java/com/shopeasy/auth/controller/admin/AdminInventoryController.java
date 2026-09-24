package com.shopeasy.auth.controller.admin;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.InventoryAdjustRequest;
import com.shopeasy.auth.dto.InventoryTransactionResponse;
import com.shopeasy.auth.dto.ProductResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> lowStock(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "stock"));
        return ResponseEntity.ok(ApiResponse.success("Low stock products retrieved successfully", inventoryService.lowStockProducts(pageable)));
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> outOfStock(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ResponseEntity.ok(ApiResponse.success("Out of stock products retrieved successfully", inventoryService.outOfStockProducts(pageable)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<InventoryTransactionResponse>>> transactions(
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success("Inventory transactions retrieved successfully",
                inventoryService.listTransactions(productId, pageable)));
    }

    @PutMapping("/products/{productId}/adjust")
    public ResponseEntity<ApiResponse<ProductResponse>> adjust(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody InventoryAdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully",
                inventoryService.adjustStock(productId, request, userDetails.getUsername())));
    }
}

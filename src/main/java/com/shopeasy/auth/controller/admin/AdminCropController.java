package com.shopeasy.auth.controller.admin;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.CropRequest;
import com.shopeasy.auth.dto.CropResponse;
import com.shopeasy.auth.dto.CropStageRequest;
import com.shopeasy.auth.dto.CropStageResponse;
import com.shopeasy.auth.service.CropService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/crops")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCropController {

    private final CropService cropService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CropResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Crops retrieved successfully", cropService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CropResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Crop retrieved successfully", cropService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CropResponse>> create(@Valid @RequestBody CropRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Crop created successfully", cropService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CropResponse>> update(@PathVariable Long id, @Valid @RequestBody CropRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Crop updated successfully", cropService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        cropService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Crop deleted successfully"));
    }

    @PostMapping("/{cropId}/stages")
    public ResponseEntity<ApiResponse<CropStageResponse>> addStage(
            @PathVariable Long cropId, @Valid @RequestBody CropStageRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Crop stage added successfully", cropService.addStage(cropId, request)));
    }

    @PutMapping("/{cropId}/stages/{stageId}")
    public ResponseEntity<ApiResponse<CropStageResponse>> updateStage(
            @PathVariable Long cropId, @PathVariable Long stageId, @Valid @RequestBody CropStageRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Crop stage updated successfully", cropService.updateStage(cropId, stageId, request)));
    }

    @DeleteMapping("/{cropId}/stages/{stageId}")
    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable Long cropId, @PathVariable Long stageId) {
        cropService.deleteStage(cropId, stageId);
        return ResponseEntity.ok(ApiResponse.success("Crop stage deleted successfully"));
    }
}

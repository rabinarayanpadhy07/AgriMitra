package com.shopeasy.auth.controller;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.CropResponse;
import com.shopeasy.auth.service.CropService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CropResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Crops retrieved successfully", cropService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CropResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Crop retrieved successfully", cropService.getById(id)));
    }
}

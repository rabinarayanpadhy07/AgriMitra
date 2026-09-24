package com.shopeasy.auth.controller;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.ReturnRequestCreateRequest;
import com.shopeasy.auth.dto.ReturnRequestResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.service.ReturnService;
import com.shopeasy.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody ReturnRequestCreateRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Return request submitted", returnService.create(user, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReturnRequestResponse>>> myReturns(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = userService.findByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "requestedAt"));
        return ResponseEntity.ok(ApiResponse.success("Return requests retrieved successfully", returnService.myReturns(user, pageable)));
    }
}

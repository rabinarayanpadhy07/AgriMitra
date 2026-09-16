package com.shopeasy.auth.controller;

import com.shopeasy.auth.dto.ApiResponse;
import com.shopeasy.auth.dto.UserProfileResponse;
import com.shopeasy.auth.entity.Session;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.service.SessionService;
import com.shopeasy.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SessionService sessionService;

    @GetMapping({"/api/user/profile", "/profile", "/me"})
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @GetMapping({"/api/user/sessions", "/sessions"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Session> sessions = sessionService.getActiveSessions(user);

        List<Map<String, Object>> response = sessions.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("loginTime", s.getLoginTime());
            map.put("expiryTime", s.getExpiryTime());
            map.put("ipAddress", s.getIpAddress());
            map.put("userAgent", s.getUserAgent());
            map.put("isActive", s.getIsActive());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Active sessions retrieved successfully", response));
    }
}

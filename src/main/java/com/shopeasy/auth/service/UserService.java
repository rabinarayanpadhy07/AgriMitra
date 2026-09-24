package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.UserProfileResponse;
import com.shopeasy.auth.entity.SellerStatus;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String email) {
        User user = findByEmail(email);
        return mapToProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse applyForSellerStatus(String email) {
        throw new ApiException("Public seller applications are closed. Products are exclusively sold and managed by administrators.");
    }

    public UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .sellerStatus(user.getSellerStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

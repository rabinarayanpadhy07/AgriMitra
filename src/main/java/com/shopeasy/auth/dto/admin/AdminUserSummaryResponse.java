package com.shopeasy.auth.dto.admin;

import com.shopeasy.auth.entity.AccountStatus;
import com.shopeasy.auth.entity.SellerStatus;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryResponse {

    private Long id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private UserRole role;
    private AccountStatus accountStatus;
    private SellerStatus sellerStatus;
    private LocalDateTime createdAt;

    public static AdminUserSummaryResponse from(User user) {
        return AdminUserSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .sellerStatus(user.getSellerStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

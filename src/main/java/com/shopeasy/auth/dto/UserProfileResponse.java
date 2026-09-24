package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.AccountStatus;
import com.shopeasy.auth.entity.SellerStatus;
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
public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private UserRole role;
    private AccountStatus accountStatus;
    private SellerStatus sellerStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

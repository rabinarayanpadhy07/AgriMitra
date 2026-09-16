package com.shopeasy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-\\[\\]{}|~`])[A-Za-z\\d@$!%*?&#^()_+=\\-\\[\\]{}|~`]{8,}$",
            message = "New password must be at least 8 characters and include at least one uppercase letter, one lowercase letter, one numeric digit, and one special character"
    )
    private String newPassword;

    @NotBlank(message = "Confirm new password is required")
    private String confirmPassword;
}

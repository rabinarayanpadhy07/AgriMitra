package com.shopeasy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Accepts either registered email address or registered 10-digit mobile number.
     */
    @NotBlank(message = "Email address or mobile number is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}

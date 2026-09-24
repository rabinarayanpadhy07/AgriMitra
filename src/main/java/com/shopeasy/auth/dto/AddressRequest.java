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
public class AddressRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;
    @NotBlank(message = "Phone is required")
    private String phone;
    @NotBlank(message = "Address line 1 is required")
    private String line1;
    private String line2;
    @NotBlank(message = "City is required")
    private String city;
    @NotBlank(message = "State is required")
    private String state;
    @NotBlank(message = "Pincode is required")
    private String pincode;
    private String country;
    private Boolean isDefault;
}

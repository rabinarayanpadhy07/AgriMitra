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
public class CropRequest {
    @NotBlank(message = "Crop name is required")
    private String name;
    private String cropCategory;
    private String description;
    private String imageUrl;
}

package com.shopeasy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequest {
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    private String ctaText;
    private String ctaLink;
    private Integer displayOrder;
    private Boolean active;
    private LocalDate startDate;
    private LocalDate endDate;
}

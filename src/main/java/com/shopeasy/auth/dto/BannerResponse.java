package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.Banner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {
    private Long id;
    private String title;
    private String imageUrl;
    private String ctaText;
    private String ctaLink;
    private Integer displayOrder;
    private Boolean active;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    public static BannerResponse from(Banner b) {
        return BannerResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .imageUrl(b.getImageUrl())
                .ctaText(b.getCtaText())
                .ctaLink(b.getCtaLink())
                .displayOrder(b.getDisplayOrder())
                .active(b.getActive())
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .createdAt(b.getCreatedAt())
                .build();
    }
}

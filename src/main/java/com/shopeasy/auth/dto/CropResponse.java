package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.Crop;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropResponse {
    private Long id;
    private String name;
    private String cropCategory;
    private String description;
    private String imageUrl;
    private List<CropStageResponse> stages;
    private LocalDateTime createdAt;

    public static CropResponse from(Crop c) {
        return CropResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .cropCategory(c.getCropCategory())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .createdAt(c.getCreatedAt())
                .build();
    }
}

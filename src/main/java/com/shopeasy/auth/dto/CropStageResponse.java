package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.CropStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropStageResponse {
    private Long id;
    private Long cropId;
    private String stageName;
    private Integer stageOrder;
    private String description;
    private Long recommendedProductId;
    private String recommendedProductName;

    public static CropStageResponse from(CropStage s) {
        return CropStageResponse.builder()
                .id(s.getId())
                .cropId(s.getCrop().getId())
                .stageName(s.getStageName())
                .stageOrder(s.getStageOrder())
                .description(s.getDescription())
                .recommendedProductId(s.getRecommendedProduct() != null ? s.getRecommendedProduct().getId() : null)
                .recommendedProductName(s.getRecommendedProduct() != null ? s.getRecommendedProduct().getName() : null)
                .build();
    }
}

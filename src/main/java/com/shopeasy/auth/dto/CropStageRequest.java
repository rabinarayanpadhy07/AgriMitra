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
public class CropStageRequest {
    @NotBlank(message = "Stage name is required")
    private String stageName;
    private Integer stageOrder;
    private String description;
    private Long recommendedProductId;
}

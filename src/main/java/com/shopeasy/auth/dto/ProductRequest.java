package com.shopeasy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    private String name;
    private String description;
    @NotBlank(message = "SKU is required")
    private String sku;
    @NotNull(message = "Price is required")
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stock;
    private Integer lowStockThreshold;
    private Boolean active;
    private Boolean featured;
    private List<String> imageUrls;
    private Long categoryId;
    private Long sellerId;

    private String crop;
    private String cropType;
    private String variety;
    private String season;
    private String soilType;
    private String growingDuration;
    private String usageInstructions;
    private String dosage;
    private String composition;
    private String manufacturer;
    private String suitableRegion;
}

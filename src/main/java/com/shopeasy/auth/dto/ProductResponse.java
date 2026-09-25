package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stock;
    private Integer lowStockThreshold;
    private String stockStatus;
    private Boolean active;
    private Boolean featured;
    private List<String> imageUrls;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;

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

    private Double averageRating;
    private Long reviewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product p) {
        String status = p.getStock() <= 0 ? "OUT_OF_STOCK"
                : p.getStock() <= p.getLowStockThreshold() ? "LOW_STOCK" : "IN_STOCK";
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .sku(p.getSku())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .stock(p.getStock())
                .lowStockThreshold(p.getLowStockThreshold())
                .stockStatus(status)
                .active(p.getActive())
                .featured(p.getFeatured())
                .imageUrls(p.getImageUrls())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .sellerId(p.getSeller() != null ? p.getSeller().getId() : null)
                .sellerName(p.getSeller() != null ? p.getSeller().getFullName() : null)
                .crop(p.getCrop())
                .cropType(p.getCropType())
                .variety(p.getVariety())
                .season(p.getSeason())
                .soilType(p.getSoilType())
                .growingDuration(p.getGrowingDuration())
                .usageInstructions(p.getUsageInstructions())
                .dosage(p.getDosage())
                .composition(p.getComposition())
                .manufacturer(p.getManufacturer())
                .suitableRegion(p.getSuitableRegion())
                .averageRating(p.getAverageRating())
                .reviewCount(p.getReviewCount() != null ? p.getReviewCount() : 0L)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

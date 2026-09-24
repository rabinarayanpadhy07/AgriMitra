package com.shopeasy.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crop_stages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @Column(name = "stage_name", nullable = false, length = 150)
    private String stageName;

    @Column(name = "stage_order", nullable = false)
    @Builder.Default
    private Integer stageOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_product_id")
    private Product recommendedProduct;
}

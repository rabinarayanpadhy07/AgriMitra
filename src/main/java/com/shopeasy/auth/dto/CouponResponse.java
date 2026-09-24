package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String code;
    private String type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private Integer usageLimit;
    private Integer perUserLimit;
    private Integer usedCount;
    private Boolean active;
    private LocalDateTime createdAt;

    public static CouponResponse from(Coupon c) {
        return CouponResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .type(c.getType().name())
                .value(c.getValue())
                .minOrderAmount(c.getMinOrderAmount())
                .maxDiscount(c.getMaxDiscount())
                .startDate(c.getStartDate())
                .expiryDate(c.getExpiryDate())
                .usageLimit(c.getUsageLimit())
                .perUserLimit(c.getPerUserLimit())
                .usedCount(c.getUsedCount())
                .active(c.getActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}

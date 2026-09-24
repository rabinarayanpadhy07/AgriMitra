package com.shopeasy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {
    @NotBlank(message = "Coupon code is required")
    private String code;
    @NotNull(message = "Coupon type is required")
    private String type;
    @NotNull(message = "Coupon value is required")
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;
    private Integer usageLimit;
    private Integer perUserLimit;
    private Boolean active;
}

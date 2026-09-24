package com.shopeasy.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    @NotNull(message = "Shipping address is required")
    private Long addressId;
    @NotNull(message = "Payment method is required")
    private String paymentMethod;
    private String couponCode;
}

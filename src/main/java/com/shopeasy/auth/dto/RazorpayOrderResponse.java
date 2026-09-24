package com.shopeasy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private Long amountInPaise;
    private BigDecimal amount;
    private String currency;
    private String keyId;
    private Long orderId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String companyName;
}

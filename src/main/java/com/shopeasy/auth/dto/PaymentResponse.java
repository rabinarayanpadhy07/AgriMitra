package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long userId;
    private String userName;
    private String method;
    private String status;
    private BigDecimal amount;
    private String transactionRef;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .userId(p.getUser().getId())
                .userName(p.getUser().getFullName())
                .method(p.getMethod().name())
                .status(p.getStatus().name())
                .amount(p.getAmount())
                .transactionRef(p.getTransactionRef())
                .createdAt(p.getCreatedAt())
                .build();
    }
}

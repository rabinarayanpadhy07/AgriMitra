package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.ReturnRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestResponse {
    private Long id;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private String userName;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;

    public static ReturnRequestResponse from(ReturnRequest r) {
        return ReturnRequestResponse.builder()
                .id(r.getId())
                .orderId(r.getOrder().getId())
                .orderItemId(r.getOrderItem() != null ? r.getOrderItem().getId() : null)
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .requestedAt(r.getRequestedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}

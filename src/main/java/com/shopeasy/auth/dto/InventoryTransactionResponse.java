package com.shopeasy.auth.dto;

import com.shopeasy.auth.entity.InventoryTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String changeType;
    private Integer quantityChange;
    private Integer resultingStock;
    private String reason;
    private String performedByName;
    private LocalDateTime createdAt;

    public static InventoryTransactionResponse from(InventoryTransaction t) {
        return InventoryTransactionResponse.builder()
                .id(t.getId())
                .productId(t.getProduct().getId())
                .productName(t.getProduct().getName())
                .changeType(t.getChangeType().name())
                .quantityChange(t.getQuantityChange())
                .resultingStock(t.getResultingStock())
                .reason(t.getReason())
                .performedByName(t.getPerformedBy() != null ? t.getPerformedBy().getFullName() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }
}

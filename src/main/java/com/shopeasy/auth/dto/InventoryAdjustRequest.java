package com.shopeasy.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustRequest {
    @NotNull(message = "Change type is required")
    private String changeType; // ADD, REMOVE, ADJUST
    @Min(value = 0, message = "Quantity must be zero or positive")
    private Integer quantity;
    private String reason;
}

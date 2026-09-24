package com.shopeasy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestCreateRequest {
    @NotNull(message = "Order id is required")
    private Long orderId;
    private Long orderItemId;
    @NotBlank(message = "Reason is required")
    private String reason;
}

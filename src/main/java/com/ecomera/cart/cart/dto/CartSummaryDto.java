package com.ecomera.cart.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Schema(description = "Lightweight cart summary for quick display")
public record CartSummaryDto(
        @Schema(description = "Unique cart ID") UUID id,
        @Schema(description = "User ID") UUID userId,
        @Schema(description = "Total price of all items") BigDecimal totalPrice,
        @Schema(description = "Total number of items (sum of quantities)") Integer totalItems,
        @Schema(description = "Number of unique products in cart") Integer itemCount
) {
}

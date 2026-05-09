package com.ecomera.cart.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record CartDto(
        @Schema(description = "Unique cart ID") UUID id,
        @Schema(description = "User ID") UUID userId,
        @Schema(description = "Cart items") List<CartItemDto> items,
        @Schema(description = "Total price of all items") BigDecimal totalPrice,
        @Schema(description = "Total number of items") Integer totalItems,
        @Schema(description = "Cart expiration time") LocalDateTime expiresAt,
        @Schema(description = "Creation timestamp") LocalDateTime createdAt,
        @Schema(description = "Last update timestamp") LocalDateTime updatedAt
) {
}

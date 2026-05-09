package com.ecomera.cart.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CartItemDto(
        @Schema(description = "Unique cart item ID") UUID id,
        @Schema(description = "Product ID") UUID productId,
        @Schema(description = "Product title") String productTitle,
        @Schema(description = "Product image URL") String productImage,
        @Schema(description = "Unit price") BigDecimal unitPrice,
        @Schema(description = "Quantity") Integer quantity,
        @Schema(description = "Available stock") Integer availableStock,
        @Schema(description = "Subtotal (unitPrice * quantity)") BigDecimal subtotal
) {
}

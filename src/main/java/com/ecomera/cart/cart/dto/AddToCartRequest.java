package com.ecomera.cart.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(description = "Request DTO for adding an item to the cart")
public record AddToCartRequest(
        @NotNull(message = "Product ID is required")
        @Schema(description = "Product ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        @Schema(description = "Quantity to add", example = "1")
        Integer quantity
) {
}

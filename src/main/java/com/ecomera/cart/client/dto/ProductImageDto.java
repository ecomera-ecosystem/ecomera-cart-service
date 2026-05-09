package com.ecomera.cart.client.dto;

import lombok.Builder;

@Builder
public record ProductImageDto(
        String imageUrl,
        Boolean isPrimary
) {
}

package com.ecomera.cart.client.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record ProductDto(
        UUID id,
        String title,
        BigDecimal price,
        Integer stock,
        List<ProductImageDto> images
) {
}

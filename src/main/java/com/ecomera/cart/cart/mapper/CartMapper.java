package com.ecomera.cart.cart.mapper;

import com.ecomera.cart.shared.common.mapper.BaseMappingConfig;
import com.ecomera.cart.cart.dto.CartDto;
import com.ecomera.cart.cart.dto.CartSummaryDto;
import com.ecomera.cart.cart.entity.Cart;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(config = BaseMappingConfig.class, uses = CartItemMapper.class)
public interface CartMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "totalPrice", expression = "java(computeTotalPrice(entity))")
    @Mapping(target = "totalItems", expression = "java(computeTotalItems(entity))")
    CartDto toDto(Cart entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "items", ignore = true)
    Cart toEntity(CartDto dto);

    @Mapping(target = "totalPrice", expression = "java(computeTotalPrice(entity))")
    @Mapping(target = "totalItems", expression = "java(computeTotalItems(entity))")
    @Mapping(target = "itemCount", expression = "java(entity.getItems() != null ? entity.getItems().size() : 0)")
    CartSummaryDto toSummaryDto(Cart entity);

    default BigDecimal computeTotalPrice(Cart entity) {
        if (entity.getItems() == null || entity.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return entity.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default Integer computeTotalItems(Cart entity) {
        if (entity.getItems() == null || entity.getItems().isEmpty()) {
            return 0;
        }
        return entity.getItems().stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
    }
}

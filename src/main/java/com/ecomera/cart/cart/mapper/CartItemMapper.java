package com.ecomera.cart.cart.mapper;

import com.ecomera.cart.shared.common.mapper.BaseMappingConfig;
import com.ecomera.cart.cart.dto.CartItemDto;
import com.ecomera.cart.cart.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(config = BaseMappingConfig.class)
public interface CartItemMapper {

    @Mapping(target = "subtotal", expression = "java(computeSubtotal(entity))")
    CartItemDto toDto(CartItem entity);

    List<CartItemDto> toDtoList(List<CartItem> entities);

    default BigDecimal computeSubtotal(CartItem entity) {
        if (entity.getUnitPrice() == null || entity.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity()));
    }
}

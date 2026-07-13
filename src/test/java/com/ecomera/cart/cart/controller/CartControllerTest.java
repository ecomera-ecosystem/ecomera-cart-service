package com.ecomera.cart.cart.controller;

import com.ecomera.cart.cart.dto.AddToCartRequest;
import com.ecomera.cart.cart.dto.CartDto;
import com.ecomera.cart.cart.dto.CartSummaryDto;
import com.ecomera.cart.cart.dto.UpdateCartItemRequest;
import com.ecomera.cart.cart.service.CartService;
import com.ecomera.cart.shared.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    private final UUID userId = UUID.randomUUID();
    private final UUID cartId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final String rolesHeader = "USER,ADMIN";

    @Test
    void addItem_shouldReturn200() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(UUID.randomUUID()).quantity(2).build();
        CartDto cartDto = CartDto.builder().id(cartId).userId(userId).items(List.of())
                .totalPrice(BigDecimal.ZERO).totalItems(0).build();

        given(cartService.addItem(eq(userId), any(AddToCartRequest.class))).willReturn(cartDto);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()));
    }

    @Test
    void addItem_shouldReturn400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCart_shouldReturn200() throws Exception {
        CartDto cartDto = CartDto.builder().id(cartId).userId(userId).items(List.of())
                .totalPrice(BigDecimal.ZERO).totalItems(0).build();
        given(cartService.getCart(userId)).willReturn(cartDto);

        mockMvc.perform(get("/api/v1/cart")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void getSummary_shouldReturn200() throws Exception {
        CartSummaryDto summary = CartSummaryDto.builder()
                .userId(userId).totalItems(2).totalPrice(BigDecimal.TEN).build();
        given(cartService.getCartSummary(userId)).willReturn(summary);

        mockMvc.perform(get("/api/v1/cart/summary")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void getCartById_shouldReturn200_whenAdmin() throws Exception {
        CartDto cartDto = CartDto.builder().id(cartId).userId(userId).items(List.of())
                .totalPrice(BigDecimal.ZERO).totalItems(0).build();
        given(cartService.getCartById(cartId)).willReturn(cartDto);

        mockMvc.perform(get("/api/v1/cart/{cartId}", cartId)
                        .header("X-User-Roles", rolesHeader))
                .andExpect(status().isOk());
    }

    @Test
    void getCartById_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/cart/{cartId}", cartId)
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCartById_shouldReturn500_whenNoRoles() throws Exception {
        mockMvc.perform(get("/api/v1/cart/{cartId}", cartId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getCartByUserId_shouldReturn200_whenAdmin() throws Exception {
        CartDto cartDto = CartDto.builder().id(cartId).userId(userId).items(List.of())
                .totalPrice(BigDecimal.ZERO).totalItems(0).build();
        given(cartService.getCartByUserId(userId)).willReturn(cartDto);

        mockMvc.perform(get("/api/v1/cart/user/{userId}", userId)
                        .header("X-User-Roles", rolesHeader))
                .andExpect(status().isOk());
    }

    @Test
    void getCartByUserId_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/cart/user/{userId}", userId)
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateItemQuantity_shouldReturn200() throws Exception {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(3).build();
        CartDto cartDto = CartDto.builder().id(cartId).userId(userId).items(List.of())
                .totalPrice(BigDecimal.ZERO).totalItems(0).build();
        given(cartService.updateItemQuantity(eq(userId), eq(itemId), any(UpdateCartItemRequest.class)))
                .willReturn(cartDto);

        mockMvc.perform(patch("/api/v1/cart/items/{itemId}", itemId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void removeItem_shouldReturn204() throws Exception {
        doNothing().when(cartService).removeItem(userId, itemId);

        mockMvc.perform(delete("/api/v1/cart/items/{itemId}", itemId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void clearCart_shouldReturn204() throws Exception {
        doNothing().when(cartService).clearCart(userId);

        mockMvc.perform(delete("/api/v1/cart")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());
    }
}

package com.ecomera.cart.cart.service;

import com.ecomera.cart.cart.dto.AddToCartRequest;
import com.ecomera.cart.cart.dto.CartDto;
import com.ecomera.cart.cart.dto.CartSummaryDto;
import com.ecomera.cart.cart.dto.UpdateCartItemRequest;
import com.ecomera.cart.cart.entity.Cart;
import com.ecomera.cart.cart.entity.CartItem;
import com.ecomera.cart.cart.mapper.CartMapper;
import com.ecomera.cart.cart.repository.CartItemRepository;
import com.ecomera.cart.cart.repository.CartRepository;
import com.ecomera.cart.client.ProductServiceClient;
import com.ecomera.cart.client.dto.ProductDto;
import com.ecomera.cart.shared.common.exception.BusinessException;
import com.ecomera.cart.shared.common.exception.ResourceNotFoundException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private CartService cartService;

    private UUID userId;
    private UUID productId;
    private UUID cartId;
    private UUID itemId;
    private AddToCartRequest addRequest;
    private UpdateCartItemRequest updateRequest;
    private ProductDto productDto;
    private Cart cart;
    private CartItem cartItem;
    private CartDto cartDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        addRequest = AddToCartRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

        updateRequest = UpdateCartItemRequest.builder()
                .quantity(3)
                .build();

        productDto = ProductDto.builder()
                .id(productId)
                .title("Test Product")
                .price(BigDecimal.valueOf(29.99))
                .stock(10)
                .images(List.of())
                .build();

        cart = Cart.builder()
                .id(cartId)
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .items(new java.util.ArrayList<>())
                .build();

        cartItem = CartItem.builder()
                .id(itemId)
                .cart(cart)
                .productId(productId)
                .productTitle("Test Product")
                .unitPrice(BigDecimal.valueOf(29.99))
                .quantity(2)
                .availableStock(10)
                .build();

        cartDto = CartDto.builder()
                .id(cartId)
                .userId(userId)
                .items(List.of())
                .totalPrice(BigDecimal.ZERO)
                .totalItems(0)
                .build();
    }

    @Test
    void addItem_shouldCreateNewCartAndAddItem() {
        given(productServiceClient.getProductById(productId)).willReturn(productDto);
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.empty());
        given(cartRepository.save(any(Cart.class))).willAnswer(invocation -> {
            Cart saved = invocation.getArgument(0);
            saved.setId(cartId);
            return saved;
        });
        given(cartMapper.toDto(any(Cart.class))).willReturn(cartDto);

        CartDto result = cartService.addItem(userId, addRequest);

        assertThat(result).isNotNull();
        verify(cartRepository, times(2)).save(any(Cart.class));
        verify(productServiceClient).getProductById(productId);
    }

    @Test
    void addItem_shouldUpdateQuantity_whenProductAlreadyInCart() {
        cart.getItems().add(cartItem);
        given(productServiceClient.getProductById(productId)).willReturn(productDto);
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndProductId(cartId, productId)).willReturn(Optional.of(cartItem));
        given(cartRepository.save(any(Cart.class))).willReturn(cart);
        given(cartMapper.toDto(any(Cart.class))).willReturn(cartDto);

        CartDto result = cartService.addItem(userId, addRequest);

        assertThat(result).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItem_shouldThrowResourceNotFoundException_whenProductNotFound() {
        given(productServiceClient.getProductById(productId)).willThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> cartService.addItem(userId, addRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    void addItem_shouldThrowBusinessException_whenFeignFails() {
        given(productServiceClient.getProductById(productId)).willThrow(mock(FeignException.class));

        assertThatThrownBy(() -> cartService.addItem(userId, addRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Product service is unavailable");
    }

    @Test
    void updateItemQuantity_shouldUpdateQuantity() {
        cart.getItems().add(cartItem);
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
        given(cartRepository.save(any(Cart.class))).willReturn(cart);
        given(cartMapper.toDto(any(Cart.class))).willReturn(cartDto);

        CartDto result = cartService.updateItemQuantity(userId, itemId, updateRequest);

        assertThat(result).isNotNull();
        assertThat(cartItem.getQuantity()).isEqualTo(3);
    }

    @Test
    void updateItemQuantity_shouldThrow_whenCartNotFound() {
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(userId, itemId, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemQuantity_shouldThrow_whenItemNotFound() {
        cart.getItems().clear();
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItemQuantity(userId, itemId, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemQuantity_shouldThrow_whenQuantityExceedsStock() {
        cartItem.setAvailableStock(1);
        cart.getItems().add(cartItem);
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItemQuantity(userId, itemId, updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds available stock");
    }

    @Test
    void removeItem_shouldRemoveItemFromCart() {
        cart.getItems().add(cartItem);
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
        given(cartRepository.save(any(Cart.class))).willReturn(cart);

        cartService.removeItem(userId, itemId);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void removeItem_shouldThrow_whenCartNotFound() {
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(userId, itemId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItem_shouldThrow_whenItemNotFound() {
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(userId, itemId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clearCart_shouldDeleteCartAndItems() {
        given(cartRepository.findByUserId(userId)).willReturn(Optional.of(cart));

        cartService.clearCart(userId);

        verify(cartItemRepository).deleteByCartId(cartId);
        verify(cartRepository).delete(cart);
    }

    @Test
    void clearCart_shouldDoNothing_whenCartNotFound() {
        given(cartRepository.findByUserId(userId)).willReturn(Optional.empty());

        cartService.clearCart(userId);

        verify(cartItemRepository, never()).deleteByCartId(any());
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void getCart_shouldReturnCart_whenExists() {
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
        given(cartMapper.toDto(cart)).willReturn(cartDto);

        CartDto result = cartService.getCart(userId);

        assertThat(result).isEqualTo(cartDto);
    }

    @Test
    void getCart_shouldReturnEmptyCart_whenNotExists() {
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.empty());

        CartDto result = cartService.getCart(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.items()).isEmpty();
        assertThat(result.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCartSummary_shouldReturnSummary_whenExists() {
        CartSummaryDto summary = CartSummaryDto.builder()
                .userId(userId).totalItems(2).totalPrice(BigDecimal.valueOf(59.98)).build();
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
        given(cartMapper.toSummaryDto(cart)).willReturn(summary);

        CartSummaryDto result = cartService.getCartSummary(userId);

        assertThat(result.totalItems()).isEqualTo(2);
    }

    @Test
    void getCartSummary_shouldReturnEmptySummary_whenNotExists() {
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.empty());

        CartSummaryDto result = cartService.getCartSummary(userId);

        assertThat(result.totalItems()).isZero();
        assertThat(result.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCartById_shouldReturnCart() {
        given(cartRepository.findByIdWithItems(cartId)).willReturn(Optional.of(cart));
        given(cartMapper.toDto(cart)).willReturn(cartDto);

        CartDto result = cartService.getCartById(cartId);

        assertThat(result).isEqualTo(cartDto);
    }

    @Test
    void getCartById_shouldThrow_whenNotFound() {
        given(cartRepository.findByIdWithItems(cartId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCartById(cartId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCartByUserId_shouldReturnCart() {
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
        given(cartMapper.toDto(cart)).willReturn(cartDto);

        CartDto result = cartService.getCartByUserId(userId);

        assertThat(result).isEqualTo(cartDto);
    }

    @Test
    void getCartByUserId_shouldReturnEmpty_whenNotExists() {
        given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.empty());

        CartDto result = cartService.getCartByUserId(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void cleanupExpiredCarts_shouldDeleteExpired() {
        List<Cart> expired = List.of(cart);
        given(cartRepository.findExpiredCarts(any(LocalDateTime.class))).willReturn(expired);

        cartService.cleanupExpiredCarts();

        verify(cartRepository).deleteAll(expired);
    }

    @Test
    void cleanupExpiredCarts_shouldDoNothing_whenNoneExpired() {
        given(cartRepository.findExpiredCarts(any(LocalDateTime.class))).willReturn(List.of());

        cartService.cleanupExpiredCarts();

        verify(cartRepository, never()).deleteAll(any());
    }
}

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
import com.ecomera.cart.client.dto.ProductImageDto;
import com.ecomera.cart.shared.common.exception.BusinessException;
import com.ecomera.cart.shared.common.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final ProductServiceClient productServiceClient;

    @Transactional
    @Caching(
            put = @CachePut(value = "cart", key = "#result.userId()"),
            evict = @CacheEvict(value = "cart-summary", key = "#userId")
    )
    public CartDto addItem(UUID userId, AddToCartRequest request) {
        ProductDto product;
        try {
            product = productServiceClient.getProductById(request.productId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Product", "id", request.productId());
        } catch (FeignException e) {
            log.error("Failed to fetch product {}: {}", request.productId(), e.getMessage());
            throw new BusinessException("Product service is unavailable. Please try again later.");
        }

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createNewCart(userId));

        cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId())
                .ifPresentOrElse(
                        existing -> updateExistingItem(existing, request, product),
                        () -> addNewItem(cart, request, product)
                );

        cart.setExpiresAt(LocalDateTime.now().plusDays(7));
        Cart saved = cartRepository.save(cart);
        log.info("Item added to cart. User: {}, Product: {}, Qty: {}", userId, product.id(), request.quantity());
        return cartMapper.toDto(saved);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "cart", key = "#result.userId()"),
            evict = @CacheEvict(value = "cart-summary", key = "#userId")
    )
    public CartDto updateItemQuantity(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Cart.class, "userId", userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(CartItem.class, "id", itemId));

        if (request.quantity() > item.getAvailableStock()) {
            throw new BusinessException(
                    "Requested quantity " + request.quantity() + " exceeds available stock " + item.getAvailableStock()
            );
        }

        item.setQuantity(request.quantity());
        Cart saved = cartRepository.save(cart);
        log.info("Cart item quantity updated. User: {}, Item: {}, Qty: {}", userId, itemId, request.quantity());
        return cartMapper.toDto(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cart", key = "#userId"),
            @CacheEvict(value = "cart-summary", key = "#userId")
    })
    public void removeItem(UUID userId, UUID itemId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Cart.class, "userId", userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(CartItem.class, "id", itemId));

        cart.getItems().remove(item);
        cartRepository.save(cart);
        log.info("Item removed from cart. User: {}, Item: {}", userId, itemId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cart", key = "#userId"),
            @CacheEvict(value = "cart-summary", key = "#userId")
    })
    public void clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
            cartRepository.delete(cart);
            log.info("Cart cleared. User: {}", userId);
        }
    }

    @Cacheable(value = "cart", key = "#userId")
    public CartDto getCart(UUID userId) {
        log.debug("Cache miss fetching cart for user {} from DB", userId);
        return cartRepository.findByUserIdWithItems(userId)
                .map(cartMapper::toDto)
                .orElseGet(() -> CartDto.builder()
                        .userId(userId)
                        .items(List.of())
                        .totalPrice(BigDecimal.ZERO)
                        .totalItems(0)
                        .build());
    }

    @Cacheable(value = "cart-summary", key = "#userId")
    public CartSummaryDto getCartSummary(UUID userId) {
        log.debug("Cache miss fetching cart summary for user {} from DB", userId);
        return cartRepository.findByUserIdWithItems(userId)
                .map(cartMapper::toSummaryDto)
                .orElseGet(() -> CartSummaryDto.builder()
                        .userId(userId)
                        .totalItems(0)
                        .totalPrice(BigDecimal.ZERO)
                        .build());
    }

    @Cacheable(value = "cart", key = "'id-' + #cartId")
    public CartDto getCartById(UUID cartId) {
        log.debug("Cache miss fetching cart by id {} from DB", cartId);
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(Cart.class, "id", cartId));
        return cartMapper.toDto(cart);
    }

    @Cacheable(value = "cart", key = "'user-' + #targetUserId")
    public CartDto getCartByUserId(UUID targetUserId) {
        log.debug("Cache miss fetching cart for user {} from DB", targetUserId);
        return cartRepository.findByUserIdWithItems(targetUserId)
                .map(cartMapper::toDto)
                .orElseGet(() -> CartDto.builder()
                        .userId(targetUserId)
                        .items(List.of())
                        .totalPrice(BigDecimal.ZERO)
                        .totalItems(0)
                        .build());
    }

    @Transactional
    @Scheduled(cron = "${cart.cleanup.cron:0 0 * * * *}")
    public void cleanupExpiredCarts() {
        log.debug("Running scheduled cleanup of expired carts");
        List<Cart> expiredCarts = cartRepository.findExpiredCarts(LocalDateTime.now());
        if (!expiredCarts.isEmpty()) {
            cartRepository.deleteAll(expiredCarts);
            log.info("Cleaned up {} expired carts", expiredCarts.size());
        }
    }

    private Cart createNewCart(UUID userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        return cartRepository.save(cart);
    }

    private void updateExistingItem(CartItem existing, AddToCartRequest request, ProductDto product) {
        int qty = request.quantity() != null ? request.quantity() : 1;
        int newQuantity = existing.getQuantity() + qty;
        if (newQuantity > product.stock()) {
            throw new BusinessException(
                    "Requested quantity " + newQuantity + " exceeds available stock " + product.stock()
            );
        }
        existing.setQuantity(newQuantity);
        existing.setUnitPrice(product.price());
        existing.setAvailableStock(product.stock());
        String imageUrl = getPrimaryImageUrl(product.images());
        if (imageUrl != null) {
            existing.setProductImage(imageUrl);
        }
    }

    private void addNewItem(Cart cart, AddToCartRequest request, ProductDto product) {
        int qty = request.quantity() != null ? request.quantity() : 1;
        if (qty > product.stock()) {
            throw new BusinessException(
                    "Requested quantity " + qty + " exceeds available stock " + product.stock()
            );
        }
        CartItem item = CartItem.builder()
                .productId(product.id())
                .productTitle(product.title())
                .unitPrice(product.price())
                .availableStock(product.stock())
                .productImage(getPrimaryImageUrl(product.images()))
                .quantity(qty)
                .cart(cart)
                .build();
        cart.getItems().add(item);
    }

    private String getPrimaryImageUrl(List<ProductImageDto> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(i -> Boolean.TRUE.equals(i.isPrimary()))
                .findFirst()
                .map(ProductImageDto::imageUrl)
                .orElse(images.get(0).imageUrl());
    }
}

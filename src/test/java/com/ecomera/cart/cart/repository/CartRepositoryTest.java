package com.ecomera.cart.cart.repository;

import com.ecomera.cart.cart.entity.Cart;
import com.ecomera.cart.cart.entity.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CartRepositoryTest.AuditingConfig.class)
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID userId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cart = Cart.builder()
                .userId(userId)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        cart = entityManager.persistAndFlush(cart);
    }

    @Test
    void findByUserId_shouldReturnCart() {
        Optional<Cart> found = cartRepository.findByUserId(userId);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenNotExists() {
        Optional<Cart> found = cartRepository.findByUserId(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void findByUserIdWithItems_shouldReturnCartWithItems() {
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(UUID.randomUUID())
                .productTitle("Test Product")
                .unitPrice(BigDecimal.valueOf(19.99))
                .quantity(2)
                .availableStock(10)
                .build();
        entityManager.persistAndFlush(item);
        entityManager.refresh(cart);

        Optional<Cart> found = cartRepository.findByUserIdWithItems(userId);
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
    }

    @Test
    void findByUserIdWithItems_shouldReturnEmpty_whenNotExists() {
        Optional<Cart> found = cartRepository.findByUserIdWithItems(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void findByIdWithItems_shouldReturnCart() {
        Optional<Cart> found = cartRepository.findByIdWithItems(cart.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(cart.getId());
    }

    @Test
    void findByIdWithItems_shouldReturnEmpty_whenNotExists() {
        Optional<Cart> found = cartRepository.findByIdWithItems(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void findExpiredCarts_shouldReturnExpired() {
        Cart expiredCart = Cart.builder()
                .userId(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        entityManager.persistAndFlush(expiredCart);

        List<Cart> expired = cartRepository.findExpiredCarts(LocalDateTime.now());
        assertThat(expired).isNotEmpty();
    }

    @Test
    void findByCartIdAndProductId_shouldReturnItem() {
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(UUID.randomUUID())
                .productTitle("Test Product")
                .unitPrice(BigDecimal.valueOf(19.99))
                .quantity(1)
                .availableStock(5)
                .build();
        entityManager.persistAndFlush(item);

        Optional<CartItem> found = cartItemRepository.findByCartIdAndProductId(
                cart.getId(), item.getProductId());
        assertThat(found).isPresent();
        assertThat(found.get().getProductTitle()).isEqualTo("Test Product");
    }

    @Test
    void countByCartId_shouldReturnCount() {
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(UUID.randomUUID())
                .productTitle("Test")
                .unitPrice(BigDecimal.TEN)
                .quantity(1)
                .availableStock(5)
                .build();
        entityManager.persistAndFlush(item);

        long count = cartItemRepository.countByCartId(cart.getId());
        assertThat(count).isEqualTo(1);
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingConfig {
        @Bean
        AuditorAware<String> auditorProvider() {
            return () -> Optional.of("test-system");
        }
    }
}

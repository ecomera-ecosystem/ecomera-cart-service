package com.ecomera.cart.cart.controller;

import com.ecomera.cart.cart.dto.AddToCartRequest;
import com.ecomera.cart.cart.dto.CartDto;
import com.ecomera.cart.cart.dto.CartSummaryDto;
import com.ecomera.cart.cart.dto.UpdateCartItemRequest;
import com.ecomera.cart.cart.service.CartService;
import com.ecomera.cart.shared.common.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Shopping cart management APIs")
public class CartController {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MANAGER_ROLE = "MANAGER";

    private final CartService cartService;

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the current user's cart.")
    @ApiResponse(responseCode = "200", description = "Item added to cart successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<CartDto> addItem(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    @GetMapping
    @Operation(summary = "Get current user's cart")
    @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Cart not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<CartDto> getCart(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get cart summary", description = "Returns lightweight totals (no item details). Useful for header badges.")
    @ApiResponse(responseCode = "200", description = "Cart summary retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Cart not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<CartSummaryDto> getSummary(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(cartService.getCartSummary(userId));
    }

    @GetMapping("/{cartId}")
    @Operation(summary = "Get cart by ID", description = "Admin/Manager: retrieve any cart by its UUID.")
    @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden — insufficient role")
    @ApiResponse(responseCode = "404", description = "Cart not found")
    public ResponseEntity<CartDto> getCartById(
            @RequestHeader("X-User-Roles") String roles,
            @Parameter(description = "Cart UUID") @PathVariable UUID cartId) {
        requireAdminOrManager(roles);
        return ResponseEntity.ok(cartService.getCartById(cartId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get cart by user ID", description = "Admin/Manager: retrieve any user's cart by their user ID.")
    @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden — insufficient role")
    @ApiResponse(responseCode = "404", description = "Cart not found")
    public ResponseEntity<CartDto> getCartByUserId(
            @RequestHeader("X-User-Roles") String roles,
            @Parameter(description = "Target user UUID") @PathVariable UUID userId) {
        requireAdminOrManager(roles);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    @ApiResponse(responseCode = "200", description = "Item quantity updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid quantity or exceeds stock")
    @ApiResponse(responseCode = "404", description = "Cart or item not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<CartDto> updateItemQuantity(
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Cart item UUID") @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    @ApiResponse(responseCode = "204", description = "Item removed successfully")
    @ApiResponse(responseCode = "404", description = "Cart or item not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<Void> removeItem(
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Cart item UUID") @PathVariable UUID itemId) {
        cartService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    @ApiResponse(responseCode = "204", description = "Cart cleared successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-User-Id") UUID userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private void requireAdminOrManager(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()
                || (!rolesHeader.contains(ADMIN_ROLE) && !rolesHeader.contains(MANAGER_ROLE))) {
            throw new ApiException("Insufficient permissions. Requires ADMIN or MANAGER role.", HttpStatus.FORBIDDEN);
        }
    }
}

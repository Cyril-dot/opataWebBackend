package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.UserPrincipal;
import com.beautyShop.Opata.Website.dto.AddToCartRequest;
import com.beautyShop.Opata.Website.dto.CartResponse;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "User Cart", description = "Cart management endpoints for customers")
public class CartController {

    private final CartService cartService;


    private UserPrincipal userPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        log.debug("Successfully retrieved UserPrincipal for user: {} (ID: {})",
                userPrincipal.getUsername(), userPrincipal.getUserId());

        return userPrincipal;
    }


    // ═══════════════════════════════════════════════════════════
    // VIEW CART
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/")
    @Operation(summary = "View cart", description = "Returns all items in the user's cart with subtotals and grand total")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cart retrieved"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResult<CartResponse>> getCart() {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("🛒 Fetching cart for user: {}", userId);
        return ResponseEntity.ok(ApiResult.success(cartService.getCart(userId)));
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count", description = "Returns the number of items in the cart — useful for badge display")
    public ResponseEntity<ApiResult<Long>> getCartItemCount() {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        return ResponseEntity.ok(ApiResult.success(cartService.getCartItemCount(userId)));
    }

    // ═══════════════════════════════════════════════════════════
    // ADD TO CART
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/add")
    @Operation(
        summary = "Add product to cart",
        description = "Adds a product to the cart. If the product is already in the cart, its quantity is increased."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product added to cart"),
        @ApiResponse(responseCode = "400", description = "Not enough stock or invalid request"),
        @ApiResponse(responseCode = "404", description = "Product or user not found")
    })
    public ResponseEntity<ApiResult<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("➕ User [{}] adding product [{}] qty [{}] to cart",
                userId, request.getProductId(), request.getQuantity());
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.ok(ApiResult.success("Product added to cart", response));
    }

    // ═══════════════════════════════════════════════════════════
    // UPDATE QUANTITY
    // ═══════════════════════════════════════════════════════════

    @PatchMapping("/items/{cartItemId}")
    @Operation(
        summary = "Update cart item quantity",
        description = "Updates the quantity of an item in the cart. Setting quantity to 0 removes the item."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quantity updated"),
        @ApiResponse(responseCode = "400", description = "Not enough stock or invalid quantity"),
        @ApiResponse(responseCode = "403", description = "Cart item does not belong to user"),
        @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<ApiResult<CartResponse>> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam @Min(value = 0, message = "Quantity must be 0 or greater") int quantity) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("✏️ User [{}] updating cart item [{}] to qty [{}]", userId, cartItemId, quantity);
        CartResponse response = cartService.updateQuantity(userId, cartItemId, quantity);
        return ResponseEntity.ok(ApiResult.success("Cart updated", response));
    }

    // ═══════════════════════════════════════════════════════════
    // REMOVE ITEM
    // ═══════════════════════════════════════════════════════════

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove a single item from cart")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item removed"),
        @ApiResponse(responseCode = "403", description = "Cart item does not belong to user"),
        @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<ApiResult<CartResponse>> removeFromCart(
            @PathVariable Long cartItemId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("🗑️ User [{}] removing cart item [{}]", userId, cartItemId);
        CartResponse response = cartService.removeFromCart(userId, cartItemId);
        return ResponseEntity.ok(ApiResult.success("Item removed from cart", response));
    }

    // ═══════════════════════════════════════════════════════════
    // CLEAR CART
    // ═══════════════════════════════════════════════════════════

    @DeleteMapping("/clear")
    @Operation(summary = "Clear entire cart", description = "Removes all items from the user's cart")
    public ResponseEntity<ApiResult<String>> clearCart() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("🧹 User [{}] clearing cart", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResult.success("Cart cleared successfully"));
    }
}
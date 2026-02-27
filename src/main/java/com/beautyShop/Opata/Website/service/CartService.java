package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.repo.CartItemRepository;
import com.beautyShop.Opata.Website.entity.repo.ProductRepository;
import com.beautyShop.Opata.Website.entity.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    // ── CartItem has its OWN repository, separate from OrderItemRepository ──
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepo userRepository;

    // ── ADD TO CART ──────────────────────────────────────────
    // If product already in cart → update quantity
    // If new product → create new CartItem row
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Not enough stock. Available: " + product.getStock());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if product already exists in this user's cart
        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        if (existing.isPresent()) {
            // Product already in cart — just increase quantity
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setUnitPrice(product.getPrice()); // refresh price in case it changed
            item.computeSubtotal();
            cartItemRepository.save(item);
            System.out.println("🛒 Cart updated: [" + product.getName() + "] qty → " + item.getQuantity());
        } else {
            // New product — create a CartItem row in cart_items table
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
            cartItemRepository.save(newItem);
            System.out.println("🛒 Added to cart: [" + product.getName() + "] qty: " + request.getQuantity());
        }

        return getCart(userId);
    }

    // ── VIEW CART ────────────────────────────────────────────
    // Returns all CartItem rows for this user with totals
    public CartResponse getCart(UUID userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);

        List<CartItemResponse> itemResponses = items.stream().map(item -> {
            BigDecimal subtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            return CartItemResponse.builder()
                    .cartItemId(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .imageUrl(item.getProduct().getPrimaryImageUrl())
                    .category(item.getProduct().getCategory())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());

        // Sum all subtotals for cart grand total
        BigDecimal cartTotal = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("🛒 Cart loaded for user [" + userId + "]: "
                + items.size() + " item(s) | Total: $" + cartTotal);

        return CartResponse.builder()
                .items(itemResponses)
                .cartTotal(cartTotal)
                .totalItems(items.size())
                .build();
    }

    // ── UPDATE ITEM QUANTITY ──────────────────────────────────
    // User changes quantity of a specific cart item
    public CartResponse updateQuantity(UUID userId, Long cartItemId, int newQuantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Security: make sure this cart item belongs to this user
        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This cart item does not belong to you");
        }

        if (newQuantity <= 0) {
            // If quantity set to 0 or less, remove the item entirely
            cartItemRepository.delete(item);
            System.out.println("🗑️  Removed from cart (qty set to 0): " + item.getProduct().getName());
        } else {
            // Check stock
            if (item.getProduct().getStock() < newQuantity) {
                throw new RuntimeException("Not enough stock. Available: " + item.getProduct().getStock());
            }
            item.setQuantity(newQuantity);
            item.computeSubtotal();
            cartItemRepository.save(item);
            System.out.println("✏️  Cart item updated: [" + item.getProduct().getName() + "] qty → " + newQuantity);
        }

        return getCart(userId);
    }

    // ── REMOVE SINGLE ITEM FROM CART ─────────────────────────
    public CartResponse removeFromCart(UUID userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This cart item does not belong to you");
        }

        cartItemRepository.delete(item);
        System.out.println("🗑️  Removed from cart: " + item.getProduct().getName());
        return getCart(userId);
    }

    // ── CLEAR ENTIRE CART ────────────────────────────────────
    // Called automatically after order is placed
    @Transactional
    public void clearCart(UUID userId) {
        cartItemRepository.deleteByUserId(userId);
        System.out.println("🧹 Cart cleared for user ID: " + userId);
    }

    // ── GET CART ITEM COUNT ───────────────────────────────────
    // Useful for showing badge count on cart icon in frontend
    public long getCartItemCount(UUID userId) {
        return cartItemRepository.countByUserId(userId);
    }
}
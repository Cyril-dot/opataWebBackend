package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OrderService — USER FACING ONLY
 * ─────────────────────────────────────────────────
 * Users can:
 *   - Place an order from their cart
 *   - View their own orders
 *   - View a single order (only if it belongs to them)
 *
 * Users CANNOT:
 *   - Update order status  → AdminOrderService
 *   - Cancel orders        → AdminOrderService
 *   - View other users' orders → AdminOrderService
 * ─────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository  productRepository;
    private final UserRepo           userRepository;
    private final CartService        cartService;

    // ── PLACE ORDER (from cart) ───────────────────────────────
    @Transactional
    public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Your cart is empty. Add items before placing an order.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = Order.builder()
                .user(user)
                .deliveryAddress(request.getDeliveryAddress())
                .status(OrderStatus.PENDING)
                .build();

        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + product.getName());
            }

            // Deduct stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal subtotal = cartItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            return OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .subtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());

        BigDecimal total = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setOrderItems(orderItems);
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        cartService.clearCart(userId);

        System.out.println("✅ Order placed! Order ID: " + saved.getId() + " | Total: $" + total);
        return mapToResponse(saved);
    }

    // ── VIEW MY ORDERS ───────────────────────────────────────
    public List<OrderResponse> getMyOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── VIEW SINGLE ORDER ────────────────────────────────────
    // Security check: order must belong to this user
    public OrderResponse getMyOrderById(Long orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This order does not belong to you");
        }

        return mapToResponse(order);
    }

    // ── PRIVATE HELPER ────────────────────────────────────────
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream().map(item ->
                OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .imageUrl(item.getProduct().getPrimaryImageUrl())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build()
        ).collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .customerEmail(order.getUser().getEmail())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .deliveryAddress(order.getDeliveryAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
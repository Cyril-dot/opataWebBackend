package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.UserPrincipal;
import com.beautyShop.Opata.Website.dto.OrderResponse;
import com.beautyShop.Opata.Website.dto.PlaceOrderRequest;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "User Orders", description = "Order endpoints for customers — place and view their own orders")
public class UserOrderController {

    private final OrderService orderService;


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
    // PLACE ORDER
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/place")
    @Operation(
        summary = "Place an order from cart",
        description = "Converts the user's cart into an order. Cart is cleared automatically after placing."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order placed successfully"),
        @ApiResponse(responseCode = "400", description = "Cart is empty or insufficient stock"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResult<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("🛒 User [{}] placing order", userId);
        OrderResponse response = orderService.placeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Order placed successfully! Order #" + response.getOrderId(), response));
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW MY ORDERS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/")
    @Operation(summary = "Get all my orders", description = "Returns all orders belonging to the logged-in user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResult<List<OrderResponse>>> getMyOrders() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("📋 Fetching orders for user: {}", userId);
        return ResponseEntity.ok(ApiResult.success(orderService.getMyOrders(userId)));
    }

    @GetMapping("/{orderId}")
    @Operation(
        summary = "Get a specific order",
        description = "Returns a single order. Only accessible if the order belongs to the requesting user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order retrieved"),
        @ApiResponse(responseCode = "403", description = "Order does not belong to this user"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResult<OrderResponse>> getMyOrderById(
            @PathVariable Long orderId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("🔍 User [{}] fetching order #{}", userId, orderId);
        return ResponseEntity.ok(ApiResult.success(orderService.getMyOrderById(orderId, userId)));
    }
}
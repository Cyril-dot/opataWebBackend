package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.dto.OrderResponse;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.entity.OrderStatus;
import com.beautyShop.Opata.Website.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Orders", description = "Admin endpoints for managing and viewing orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    // ═══════════════════════════════════════════════════════════
    // STATUS MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Update an order status: PENDING → CONFIRMED → SHIPPED → DELIVERED")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResult<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam @Parameter(description = "New status: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED") OrderStatus status) {

        log.info("🔄 [ADMIN] Updating order #{} to status: {}", orderId, status);
        OrderResponse response = adminOrderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResult.success("Order status updated to " + status, response));
    }

    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order", description = "Admin cancels an order (cannot cancel already delivered orders)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order cancelled"),
        @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResult<OrderResponse>> cancelOrder(@PathVariable Long orderId) {
        log.info("❌ [ADMIN] Cancelling order #{}", orderId);
        OrderResponse response = adminOrderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResult.success("Order #" + orderId + " cancelled successfully", response));
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW ALL ORDERS
    // ═══════════════════════════════════════════════════════════

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieve all orders sorted by newest first")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getAllOrders() {
        log.info("📋 [ADMIN] Fetching all orders");
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getAllOrders()));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResult<OrderResponse>> getOrderById(@PathVariable Long orderId) {
        log.info("🔍 [ADMIN] Fetching order #{}", orderId);
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrderById(orderId)));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent orders", description = "Returns the N most recent orders")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getRecentOrders(limit)));
    }

    @GetMapping("/highest-value")
    @Operation(summary = "Get highest value orders", description = "Returns orders sorted by total amount descending")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getHighestValueOrders(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getHighestValueOrders(limit)));
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW ORDERS BY STATUS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        log.info("📋 [ADMIN] Fetching {} orders", status);
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrdersByStatus(status)));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all pending orders")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getPendingOrders() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getPendingOrders()));
    }

    @GetMapping("/confirmed")
    @Operation(summary = "Get all confirmed orders")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getConfirmedOrders() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getConfirmedOrders()));
    }

    @GetMapping("/shipped")
    @Operation(summary = "Get all shipped orders")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getShippedOrders() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getShippedOrders()));
    }

    @GetMapping("/delivered")
    @Operation(summary = "Get all delivered orders")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getDeliveredOrders() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getDeliveredOrders()));
    }

    @GetMapping("/cancelled")
    @Operation(summary = "Get all cancelled orders")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getCancelledOrders() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getCancelledOrders()));
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW ORDERS BY DATE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/today")
    @Operation(summary = "Get today's orders")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getOrdersToday() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrdersToday()));
    }

    @GetMapping("/this-week")
    @Operation(summary = "Get orders from the last 7 days")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getOrdersThisWeek() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrdersThisWeek()));
    }

    @GetMapping("/this-month")
    @Operation(summary = "Get orders from the current month")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getOrdersThisMonth() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrdersThisMonth()));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get orders within a custom date range")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.info("📅 [ADMIN] Fetching orders from {} to {}", from, to);
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrdersByDateRange(from, to)));
    }

    // ═══════════════════════════════════════════════════════════
    // VIEW ORDERS BY USER
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all orders for a specific user")
    public ResponseEntity<ApiResult<List<OrderResponse>>> getOrdersByUser(@PathVariable UUID userId) {
        log.info("👤 [ADMIN] Fetching orders for user: {}", userId);
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrdersByUser(userId)));
    }

    // ═══════════════════════════════════════════════════════════
    // DASHBOARD SUMMARY
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/summary")
    @Operation(summary = "Get order dashboard summary", description = "Returns totals, revenue, and counts per status")
    public ResponseEntity<ApiResult<Map<String, Object>>> getOrderSummary() {
        log.info("📊 [ADMIN] Fetching order summary");
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrderSummary()));
    }

    @GetMapping("/summary/per-day")
    @Operation(summary = "Get order count per day for the last 7 days")
    public ResponseEntity<ApiResult<Map<String, Long>>> getOrderCountPerDayLastWeek() {
        return ResponseEntity.ok(ApiResult.success(adminOrderService.getOrderCountPerDayLastWeek()));
    }
}
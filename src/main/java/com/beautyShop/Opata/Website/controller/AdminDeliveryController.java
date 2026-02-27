package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.AdminPrincipal;
import com.beautyShop.Opata.Website.dto.ChatRoomResponse;
import com.beautyShop.Opata.Website.dto.DeliveryResponse;
import com.beautyShop.Opata.Website.dto.DeliveryStatusUpdateRequest;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.entity.DeliveryStatus;
import com.beautyShop.Opata.Website.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Deliveries", description = "Admin endpoints for managing and updating deliveries")
class AdminDeliveryController {

    private final DeliveryService deliveryService;

    private AdminPrincipal adminPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("User not authenticated");
        Object principal = auth.getPrincipal();
        if (!(principal instanceof AdminPrincipal)) throw new RuntimeException("Invalid authentication principal");
        return (AdminPrincipal) principal;
    }

    @GetMapping
    @Operation(summary = "Get all deliveries")
    public ResponseEntity<ApiResult<List<DeliveryResponse>>> getAllDeliveries() {
        log.info("📋 [ADMIN] Fetching all deliveries");
        return ResponseEntity.ok(ApiResult.success(deliveryService.getAllDeliveries()));
    }

    @GetMapping("/{deliveryId}")
    @Operation(summary = "Get a single delivery by ID")
    public ResponseEntity<ApiResult<DeliveryResponse>> getDeliveryById(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(ApiResult.success(deliveryService.getDeliveryById(deliveryId)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get deliveries by status")
    public ResponseEntity<ApiResult<List<DeliveryResponse>>> getDeliveriesByStatus(
            @PathVariable DeliveryStatus status) {
        return ResponseEntity.ok(ApiResult.success(deliveryService.getDeliveriesByStatus(status)));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active deliveries (not delivered or cancelled)")
    public ResponseEntity<ApiResult<List<DeliveryResponse>>> getActiveDeliveries() {
        return ResponseEntity.ok(ApiResult.success(deliveryService.getActiveDeliveries()));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue deliveries", description = "Deliveries past their estimated delivery time")
    public ResponseEntity<ApiResult<List<DeliveryResponse>>> getOverdueDeliveries() {
        return ResponseEntity.ok(ApiResult.success(deliveryService.getOverdueDeliveries()));
    }

    @GetMapping("/assigned")
    @Operation(summary = "Get all deliveries assigned to a specific admin")
    public ResponseEntity<ApiResult<List<DeliveryResponse>>> getMyAssignedDeliveries() {
        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();
        return ResponseEntity.ok(ApiResult.success(deliveryService.getMyAssignedDeliveries(adminId)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get the delivery for a specific order")
    public ResponseEntity<ApiResult<List<DeliveryResponse>>> getDeliveriesForOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResult.success(deliveryService.getDeliveriesForOrder(orderId)));
    }

    @PatchMapping("/{deliveryId}/status")
    @Operation(
            summary = "Update delivery status",
            description = "Updates the status and optionally sets tracking number, courier, fee, estimated time, " +
                    "and sends a message to the user via the delivery chat"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<ApiResult<DeliveryResponse>> updateDeliveryStatus(
            @PathVariable Long deliveryId,
            @Valid @RequestBody DeliveryStatusUpdateRequest request) {

        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();

        log.info("📦 [ADMIN] Updating delivery #{} to status: {}", deliveryId, request.getStatus());
        DeliveryResponse response = deliveryService.updateDeliveryStatus(deliveryId, adminId, request);
        return ResponseEntity.ok(ApiResult.success("Delivery #" + deliveryId + " updated to " + request.getStatus(), response));
    }

    @PostMapping("/{deliveryId}/chat")
    @Operation(
            summary = "Open a chat with the customer about a delivery",
            description = "Creates a delivery chat room (or returns the existing one) so admin can message the customer"
    )
    public ResponseEntity<ApiResult<ChatRoomResponse>> openDeliveryChat(
            @PathVariable Long deliveryId) {

        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();

        log.info("💬 [ADMIN] Opening delivery chat for delivery #{}", deliveryId);
        ChatRoomResponse response = deliveryService.openDeliveryChat(adminId, deliveryId);
        return ResponseEntity.ok(ApiResult.success("Delivery chat opened", response));
    }

    @PatchMapping("/{deliveryId}/cancel")
    @Operation(summary = "Cancel a delivery (admin)", description = "Admin can cancel a delivery if it hasn't been picked up")
    public ResponseEntity<ApiResult<DeliveryResponse>> cancelDelivery(
            @PathVariable Long deliveryId) {

        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();

        log.info("❌ [ADMIN] Cancelling delivery #{}", deliveryId);
        DeliveryResponse response = deliveryService.cancelDelivery(deliveryId, adminId);
        return ResponseEntity.ok(ApiResult.success("Delivery #" + deliveryId + " cancelled", response));
    }
}

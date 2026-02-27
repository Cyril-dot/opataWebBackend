package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.UserPrincipal;
import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.service.DeliveryService;
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

// ═══════════════════════════════════════════════════════════════
// USER DELIVERY CONTROLLER
// ═══════════════════════════════════════════════════════════════

@Slf4j
@RestController
@RequestMapping("/api/user/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "User Deliveries", description = "Delivery endpoints for customers — request and track deliveries")
class UserDeliveryController {

    private final DeliveryService deliveryService;


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


    @PostMapping("/request")
    @Operation(
        summary = "Request a delivery",
        description = "User submits a delivery request for one of their orders. Only one delivery per order is allowed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Delivery request submitted"),
        @ApiResponse(responseCode = "400", description = "Duplicate delivery request or cancelled order"),
        @ApiResponse(responseCode = "403", description = "Order does not belong to this user"),
        @ApiResponse(responseCode = "404", description = "Order or user not found")
    })
    public ResponseEntity<ApiResult<DeliveryResponse>> requestDelivery(
            @Valid @RequestBody DeliveryRequest request) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("🚚 User [{}] requesting delivery for order #{}", userId, request.getOrderId());
        DeliveryResponse response = deliveryService.requestDelivery(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Delivery request submitted successfully!", response));
    }

    @GetMapping("/")
    @Operation(summary = "Get all my deliveries")
    public ResponseEntity<ApiResult<List<DeliveryResponse>>> getMyDeliveries() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("📋 Fetching deliveries for user: {}", userId);
        return ResponseEntity.ok(ApiResult.success(deliveryService.getMyDeliveries(userId)));
    }

    @GetMapping("/{deliveryId}")
    @Operation(summary = "Get a specific delivery", description = "Only accessible if the delivery belongs to this user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery found"),
        @ApiResponse(responseCode = "403", description = "Delivery does not belong to this user"),
        @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<ApiResult<DeliveryResponse>> getMyDeliveryById(
            @PathVariable Long deliveryId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        return ResponseEntity.ok(ApiResult.success(deliveryService.getMyDeliveryById(userId, deliveryId)));
    }

    @GetMapping("/track/{trackingNumber}")
    @Operation(summary = "Track delivery by tracking number", description = "Public endpoint — no auth needed")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResult<DeliveryResponse>> trackDelivery(
            @PathVariable String trackingNumber) {

        log.info("🔍 Tracking delivery: {}", trackingNumber);
        return ResponseEntity.ok(ApiResult.success(deliveryService.trackDelivery(trackingNumber)));
    }

    @PatchMapping("/{deliveryId}/cancel")
    @Operation(
        summary = "Cancel a delivery",
        description = "User can cancel their delivery if it has not been picked up yet"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery cancelled"),
        @ApiResponse(responseCode = "400", description = "Cannot cancel — delivery in transit or already delivered"),
        @ApiResponse(responseCode = "403", description = "Not your delivery")
    })
    public ResponseEntity<ApiResult<DeliveryResponse>> cancelDelivery(
            @PathVariable Long deliveryId) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("❌ User [{}] cancelling delivery #{}", userId, deliveryId);
        DeliveryResponse response = deliveryService.cancelDelivery(deliveryId, userId);
        return ResponseEntity.ok(ApiResult.success("Delivery cancelled", response));
    }
}


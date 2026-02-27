package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.AdminPrincipal;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.service.CloudinaryService;
import com.beautyShop.Opata.Website.service.TelegramBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/telegram")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Telegram", description = "Admin endpoints for sending messages and managing Telegram bot")
public class TelegramAdminController {

    private final TelegramBotService telegramBotService;
    private final CloudinaryService cloudinaryService;

    // ═══════════════════════════════════════════════════════════
    // ADMIN REGISTRATION
    // ═══════════════════════════════════════════════════════════

    private AdminPrincipal adminPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("User not authenticated");
        Object principal = auth.getPrincipal();
        if (!(principal instanceof AdminPrincipal)) throw new RuntimeException("Invalid authentication principal");
        return (AdminPrincipal) principal;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register admin's Telegram chat ID",
            description = "Links the admin's Telegram account to the bot so it can receive notifications."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid secret or missing fields")
    })
    public ResponseEntity<ApiResult<String>> registerAdmin(
            @RequestParam @NotNull(message = "chatId is required") Long chatId,
            @RequestParam @NotBlank(message = "secret is required") String secret) {

        log.info("🔐 Admin registration attempt for chatId: {}", chatId);
        telegramBotService.registerAdmin(chatId, secret);
        return ResponseEntity.ok(ApiResult.success("Admin registration processed. Check your Telegram for confirmation."));
    }

    // ═══════════════════════════════════════════════════════════
    // BROADCAST TO CHANNEL
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/channel/broadcast")
    @Operation(summary = "Send a broadcast message to the Telegram channel")
    public ResponseEntity<ApiResult<String>> broadcastToChannel(
            @RequestParam @NotBlank(message = "Message text cannot be blank") String message) {

        log.info("📢 [ADMIN] Broadcasting message to Telegram channel");
        telegramBotService.broadcastToChannel(message);
        return ResponseEntity.ok(ApiResult.success("Broadcast sent to channel successfully"));
    }

    @PostMapping("/channel/sale")
    @Operation(summary = "Post a sale announcement to the Telegram channel")
    public ResponseEntity<ApiResult<String>> broadcastSale(
            @RequestParam @NotBlank(message = "Sale name is required") String saleName,
            @RequestParam @NotBlank(message = "Discount percentage is required") String discount,
            @RequestParam @NotBlank(message = "End date is required") String endDate) {

        log.info("🔥 [ADMIN] Broadcasting sale: {} ({}% off) until {}", saleName, discount, endDate);
        telegramBotService.broadcastSaleToChannel(saleName, discount, endDate);
        return ResponseEntity.ok(ApiResult.success("Sale announcement sent to channel successfully"));
    }

    // ═══════════════════════════════════════════════════════════
    // MANUAL NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/notify/order-status")
    @Operation(summary = "Manually send an order status update notification to admin Telegram")
    public ResponseEntity<ApiResult<String>> notifyOrderStatus(
            @RequestParam @NotBlank(message = "Customer name is required") String customerName,
            @RequestParam @NotNull(message = "Order ID is required") @Positive Long orderId,
            @RequestParam @NotBlank(message = "Old status is required") String oldStatus,
            @RequestParam @NotBlank(message = "New status is required") String newStatus) {

        log.info("📦 [ADMIN] Sending manual order status notification for order #{}", orderId);
        telegramBotService.notifyOrderStatusUpdate(customerName, orderId, oldStatus, newStatus);
        return ResponseEntity.ok(ApiResult.success("Order status notification sent"));
    }

    @PostMapping("/notify/delivery-request")
    @Operation(summary = "Manually send a new delivery request notification to admin Telegram")
    public ResponseEntity<ApiResult<String>> notifyDeliveryRequest(
            @RequestParam @NotBlank(message = "Customer name is required") String customerName,
            @RequestParam @NotNull(message = "Delivery ID is required") @Positive Long deliveryId,
            @RequestParam @NotBlank(message = "Address is required") String address,
            @RequestParam(required = false) String city) {

        log.info("🚚 [ADMIN] Sending delivery request notification for delivery #{}", deliveryId);
        telegramBotService.notifyNewDeliveryRequest(customerName, deliveryId, address, city);
        return ResponseEntity.ok(ApiResult.success("Delivery request notification sent"));
    }

    @PostMapping("/notify/delivery-status")
    @Operation(summary = "Manually send a delivery status update notification to admin Telegram")
    public ResponseEntity<ApiResult<String>> notifyDeliveryStatus(
            @RequestParam @NotBlank(message = "Customer name is required") String customerName,
            @RequestParam @NotNull(message = "Delivery ID is required") @Positive Long deliveryId,
            @RequestParam @NotBlank(message = "New status is required") String newStatus,
            @RequestParam(required = false) String trackingNumber) {

        log.info("📦 [ADMIN] Sending delivery status update for delivery #{}", deliveryId);
        telegramBotService.notifyDeliveryStatusUpdate(customerName, deliveryId, newStatus, trackingNumber);
        return ResponseEntity.ok(ApiResult.success("Delivery status notification sent"));
    }

    // ═══════════════════════════════════════════════════════════
    // PRODUCT ANNOUNCEMENTS
    //
    // KEY FACTS:
    //   • productId is NOT a @RequestParam — it is generated here via
    //     System.currentTimeMillis(). The client never sends it.
    //   • image is a @RequestPart MultipartFile — the client uploads
    //     the actual file; this controller pushes it to Cloudinary and
    //     passes the resulting URL to the Telegram service.
    //   • No imageUrl string param exists anywhere.
    // ═══════════════════════════════════════════════════════════

    @PostMapping(value = "/channel/announce-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Announce a clothing product to the Telegram channel",
            description = "Upload the product image as a file. Product ID is auto-generated."
    )
    public ResponseEntity<ApiResult<String>> announceClothingProduct(
            @RequestParam @NotBlank(message = "Product name is required")   String name,
            @RequestParam(required = false)                                  String description,
            @RequestParam @NotBlank(message = "Price is required")           String price,
            @RequestParam @NotBlank(message = "Category is required")        String category,
            @RequestParam(required = false)                                  String subCategory,
            @RequestParam(defaultValue = "N/A")                              String sizes,
            @RequestParam(defaultValue = "N/A")                              String colors,
            @RequestParam(required = false)                                  String material,
            @RequestParam(required = false)                                  String style,
            // ↓ actual file — NOT a URL string
            @RequestPart(value = "image", required = false)                  MultipartFile image) {

        Long productId = System.currentTimeMillis();           // ← auto-generated
        String imageUrl = uploadImageAndGetUrl(image, "products/clothing");

        log.info("📢 [ADMIN] Announcing clothing '{}' (auto-ID: {})", name, productId);
        telegramBotService.announceNewProduct(
                name, description, price, category, subCategory,
                sizes, colors, material, style, imageUrl, productId);

        return ResponseEntity.ok(ApiResult.success(
                "Clothing product announced on Telegram (ID: " + productId + ")"));
    }

    @PostMapping(value = "/channel/announce-general-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Announce a general product to the Telegram channel",
            description = "Upload the product image as a file. Product ID is auto-generated."
    )
    public ResponseEntity<ApiResult<String>> announceGeneralProduct(
            @RequestParam @NotBlank(message = "Product name is required") String name,
            @RequestParam(required = false)                                String description,
            @RequestParam @NotBlank(message = "Price is required")         String price,
            @RequestParam @NotBlank(message = "Category is required")      String category,
            @RequestParam(required = false)                                String subCategory,
            @RequestParam(required = false)                                String tags,
            // ↓ actual file — NOT a URL string
            @RequestPart(value = "image", required = false)                MultipartFile image) {

        Long productId = System.currentTimeMillis();           // ← auto-generated
        String imageUrl = uploadImageAndGetUrl(image, "products/general");

        log.info("📢 [ADMIN] Announcing general product '{}' (auto-ID: {})", name, productId);
        telegramBotService.announceNewGeneralProduct(
                name, description, price, category, subCategory, tags, imageUrl, productId);

        return ResponseEntity.ok(ApiResult.success(
                "General product announced on Telegram (ID: " + productId + ")"));
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPER
    // ═══════════════════════════════════════════════════════════

    /**
     * Uploads a file to Cloudinary and returns the secure_url.
     * Returns null gracefully so the announcement still posts without an image.
     */
    private String uploadImageAndGetUrl(MultipartFile image, String folder) {
        if (image == null || image.isEmpty()) {
            log.info("ℹ️ No image provided — announcement will proceed without image.");
            return null;
        }
        try {
            Map<?, ?> result = cloudinaryService.uploadImage(image, folder);
            String url = (String) result.get("secure_url");
            log.info("✅ Image uploaded to Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            log.error("❌ Cloudinary upload failed: {}", e.getMessage());
            return null;
        }
    }
}
package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.AdminPrincipal;
import com.beautyShop.Opata.Website.dto.ChatMessageResponse;
import com.beautyShop.Opata.Website.dto.ChatRoomResponse;
import com.beautyShop.Opata.Website.dto.SendMessageRequest;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Chat", description = "Chat endpoints for admins — start order/delivery chats and reply to users")
class AdminChatController {

    private final ChatService chatService;

    private AdminPrincipal adminPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("User not authenticated");
        Object principal = auth.getPrincipal();
        if (!(principal instanceof AdminPrincipal)) throw new RuntimeException("Invalid authentication principal");
        return (AdminPrincipal) principal;
    }

    // ── Start an order chat ───────────────────────────────────

    @PostMapping("/order/{orderId}/start")
    @Operation(
            summary = "Open a chat with a customer about their order",
            description = "Creates an order chat room or returns existing one. User is notified by email."
    )
    public ResponseEntity<ApiResult<ChatRoomResponse>> startOrderChat(
            @PathVariable Long orderId) {

        AdminPrincipal adminPrincipal = adminPrincipal();
        UUID adminId = adminPrincipal.getOwnerId();

        log.info("💬 [ADMIN] Opening order chat for order #{}", orderId);
        ChatRoomResponse response = chatService.startOrderChat(adminId, orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Order chat opened", response));
    }

    // ── Start a delivery chat ─────────────────────────────────

    @PostMapping("/delivery/{deliveryId}/start")
    @Operation(
            summary = "Open a chat with a customer about their delivery",
            description = "Creates a delivery chat room or returns existing one. User is notified by email."
    )
    public ResponseEntity<ApiResult<ChatRoomResponse>> startDeliveryChat(
            @PathVariable Long deliveryId) {

        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();

        log.info("💬 [ADMIN] Opening delivery chat for delivery #{}", deliveryId);
        ChatRoomResponse response = chatService.startDeliveryChat(adminId, deliveryId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Delivery chat opened", response));
    }

    // ── Send a message ────────────────────────────────────────

    @PostMapping(value = "/rooms/{chatRoomId}/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Send a message to a user in a chat room",
            description = "Admin sends a text, image, or video message. User is notified by email and WebSocket."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message sent"),
            @ApiResponse(responseCode = "404", description = "Chat room or admin not found")
    })
    public ResponseEntity<ApiResult<ChatMessageResponse>> sendMessage(
            @PathVariable Long chatRoomId,
            @RequestPart("message") @Valid SendMessageRequest request,
            @RequestPart(value = "media", required = false) MultipartFile mediaFile) throws IOException {

        UUID adminId = adminPrincipal().getOwnerId();

        log.info("📨 [ADMIN] Sending message in chat #{}", chatRoomId);
        ChatMessageResponse response = chatService.adminSendMessage(adminId, chatRoomId, request, mediaFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Message sent", response));
    }

    // ── View chat history ─────────────────────────────────────

    @GetMapping("/rooms/{chatRoomId}/history")
    @Operation(summary = "Get full message history for a chat room")
    public ResponseEntity<ApiResult<List<ChatMessageResponse>>> getChatHistory(
            @PathVariable Long chatRoomId) {
        return ResponseEntity.ok(ApiResult.success(chatService.getChatHistory(chatRoomId)));
    }

    @GetMapping("/rooms/{chatRoomId}")
    @Operation(summary = "Get a chat room by ID")
    public ResponseEntity<ApiResult<ChatRoomResponse>> getChatRoomById(@PathVariable Long chatRoomId) {
        return ResponseEntity.ok(ApiResult.success(chatService.getChatRoomById(chatRoomId)));
    }

    // ── View admin's chat rooms ───────────────────────────────

    @GetMapping("/rooms")
    @Operation(summary = "Get all chat rooms assigned to this admin")
    public ResponseEntity<ApiResult<List<ChatRoomResponse>>> getAdminChatRooms() {
        AdminPrincipal adminPrincipal = adminPrincipal();
        UUID adminId = adminPrincipal.getOwnerId();
        return ResponseEntity.ok(ApiResult.success(chatService.getAdminChatRooms(adminId)));
    }

    @GetMapping("/rooms/orders")
    @Operation(summary = "Get all order chats assigned to this admin")
    public ResponseEntity<ApiResult<List<ChatRoomResponse>>> getAdminOrderChats() {
        AdminPrincipal adminPrincipal = adminPrincipal();
        UUID adminId = adminPrincipal.getOwnerId();
        return ResponseEntity.ok(ApiResult.success(chatService.getAdminOrderChats(adminId)));
    }

    @GetMapping("/rooms/deliveries")
    @Operation(summary = "Get all delivery chats assigned to this admin")
    public ResponseEntity<ApiResult<List<ChatRoomResponse>>> getAdminDeliveryChats() {
        AdminPrincipal adminPrincipal = adminPrincipal();
        UUID adminId = adminPrincipal.getOwnerId();
        return ResponseEntity.ok(ApiResult.success(chatService.getAdminDeliveryChats(adminId)));
    }

    // ── Delete a message ──────────────────────────────────────

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Delete a message", description = "Admins can only delete their own messages")
    public ResponseEntity<ApiResult<String>> deleteMessage(
            @PathVariable Long messageId) throws IOException {

        AdminPrincipal adminPrincipal = adminPrincipal();
        UUID adminId = adminPrincipal.getOwnerId();
        log.info("🗑️ [ADMIN] Deleting message #{}", messageId);
        chatService.deleteMessage(messageId, adminId);
        return ResponseEntity.ok(ApiResult.success("Message deleted successfully"));
    }
}

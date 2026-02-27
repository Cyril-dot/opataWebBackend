package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.UserPrincipal;
import com.beautyShop.Opata.Website.dto.*;
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


// ═══════════════════════════════════════════════════════════════
// USER CHAT CONTROLLER
// ═══════════════════════════════════════════════════════════════

@Slf4j
@RestController
@RequestMapping("/api/user/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "User Chat", description = "Chat endpoints for customers — start product chats and send messages")
class UserChatController {

    private final ChatService chatService;

    // ── Start a product enquiry chat ─────────────────────────

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


    @PostMapping("/product/start")
    @Operation(
        summary = "Start a product enquiry chat",
        description = "Creates a new chat room for a product enquiry. Returns existing room if one already exists."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Chat room created or existing room returned"),
        @ApiResponse(responseCode = "404", description = "Product or user not found")
    })
    public ResponseEntity<ApiResult<ChatRoomResponse>> startProductChat(
            @Valid @RequestBody StartChatRequest request) {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("💬 User [{}] starting product chat for product #{}", userId, request.getProductId());
        ChatRoomResponse response = chatService.startProductChat(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Chat started", response));
    }

    // ── Send a message (text, image, or video) ────────────────

    @PostMapping(value = "/rooms/{chatRoomId}/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Send a message in a chat room",
        description = "Sends a text, image, or video message. Media is uploaded to Cloudinary. " +
                      "Admin is notified by email and WebSocket."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Message sent"),
        @ApiResponse(responseCode = "403", description = "Chat room does not belong to this user"),
        @ApiResponse(responseCode = "404", description = "Chat room not found")
    })
    public ResponseEntity<ApiResult<ChatMessageResponse>> sendMessage(
            @PathVariable Long chatRoomId,
            @RequestPart("message") @Valid SendMessageRequest request,
            @RequestPart(value = "media", required = false) MultipartFile mediaFile) throws IOException {

        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();

        log.info("📨 User [{}] sending message in chat #{}", userId, chatRoomId);
        ChatMessageResponse response = chatService.userSendMessage(userId, chatRoomId, request, mediaFile);
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

    // ── View user's chat rooms ────────────────────────────────

    @GetMapping("/rooms")
    @Operation(summary = "Get all chat rooms for this user")
    public ResponseEntity<ApiResult<List<ChatRoomResponse>>> getUserChatRooms() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        return ResponseEntity.ok(ApiResult.success(chatService.getUserChatRooms(userId)));
    }

    @GetMapping("/rooms/products")
    @Operation(summary = "Get product enquiry chats for this user")
    public ResponseEntity<ApiResult<List<ChatRoomResponse>>> getUserProductChats() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        return ResponseEntity.ok(ApiResult.success(chatService.getUserProductChats(userId)));
    }

    @GetMapping("/rooms/orders")
    @Operation(summary = "Get order chats for this user")
    public ResponseEntity<ApiResult<List<ChatRoomResponse>>> getUserOrderChats() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        return ResponseEntity.ok(ApiResult.success(chatService.getUserOrderChats(userId)));
    }

    @GetMapping("/rooms/deliveries")
    @Operation(summary = "Get delivery chats for this user")
    public ResponseEntity<ApiResult<List<ChatRoomResponse>>> getUserDeliveryChats() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        return ResponseEntity.ok(ApiResult.success(chatService.getUserDeliveryChats(userId)));
    }

    // ── Delete a message ──────────────────────────────────────

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Delete a message", description = "Users can only delete their own messages")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message deleted"),
        @ApiResponse(responseCode = "403", description = "Cannot delete another user's message"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<ApiResult<String>> deleteMessage(
            @PathVariable Long messageId) throws IOException {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("🗑️ User [{}] deleting message #{}", userId, messageId);
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok(ApiResult.success("Message deleted successfully"));
    }
}


// ═══════════════════════════════════════════════════════════════
// ADMIN CHAT CONTROLLER
// ═══════════════════════════════════════════════════════════════


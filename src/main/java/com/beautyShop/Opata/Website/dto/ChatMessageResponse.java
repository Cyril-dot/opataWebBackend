package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// ── Returned when viewing a single chat message ──────────────
@Data
@Builder
public class ChatMessageResponse {

    private Long messageId;
    private Long chatRoomId;
    private String senderType;   // "USER" or "ADMIN"
    private String senderName;
    private String content;      // text content or caption
    private String messageType;  // "TEXT", "IMAGE", "VIDEO"
    private String mediaUrl;     // Cloudinary URL — null for TEXT messages
    private boolean isProductCard;
    private boolean isOrderCard;
    private Long linkedOrderId;  // non-null if this message is about an order
    private LocalDateTime sentAt;
}
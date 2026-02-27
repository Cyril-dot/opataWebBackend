package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// ── Returned when viewing a chat room ────────────────────────
@Data
@Builder
public class ChatRoomResponse {

    private Long chatRoomId;
    private String title;
    private String roomType;       // "PRODUCT" or "ORDER"

    // Product info (present for PRODUCT rooms)
    private Long productId;
    private String productName;
    private String productImage;
    private String productPrice;

    // Order info (present for ORDER rooms)
    private Long orderId;
    private String orderStatus;
    private String orderTotal;

    private String customerName;
    private String customerEmail;
    private String adminName;
    private LocalDateTime createdAt;

    private Long deliveryId;
    private String deliveryStatus;
    private String deliveryAddress;
}
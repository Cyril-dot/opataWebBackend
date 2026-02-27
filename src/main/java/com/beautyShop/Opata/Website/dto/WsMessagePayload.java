package com.beautyShop.Opata.Website.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Payload received from frontend via WebSocket (STOMP).
 * Frontend sends JSON: { "senderId": "uuid-or-id", "content": "Hello!" }
 */
@Data
public class WsMessagePayload {
    private UUID senderId; // UUID for user, Long for admin
    private String content;
}
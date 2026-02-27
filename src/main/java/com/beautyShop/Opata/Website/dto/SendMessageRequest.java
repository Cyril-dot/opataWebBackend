package com.beautyShop.Opata.Website.dto;

import lombok.Data;

// ── Used when user or admin sends any message ────────────────
@Data
public class SendMessageRequest {

    // Text content — required for TEXT, optional caption for IMAGE/VIDEO
    private String content;

    // "TEXT", "IMAGE", "VIDEO" — defaults to TEXT if not provided
    private String messageType;

    // Media public ID from Cloudinary (after frontend uploads directly to Cloudinary)
    // Only required for IMAGE and VIDEO messages
    private String mediaUrl;
    private String mediaPublicId;
}
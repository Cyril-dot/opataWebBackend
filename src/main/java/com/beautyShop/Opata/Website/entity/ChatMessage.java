package com.beautyShop.Opata.Website.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    private SenderType senderType; // USER or ADMIN

    private String senderId;
    private String senderName;

    // ── Message content ──────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String content;  // text content (null if media-only message)

    // ── Media support ────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT; // TEXT, IMAGE, VIDEO

    private String mediaUrl;       // Cloudinary URL for image or video
    private String mediaPublicId;  // Cloudinary public ID for deletion

    // ── Special message flags ────────────────────────────────
    @Builder.Default
    private boolean isProductCard = false;  // auto-generated product card on chat start

    @Builder.Default
    private boolean isOrderCard = false;    // admin-initiated order notification card

    // ── Order link (optional) ─────────────────────────────────
    // Set when this message is linked to a specific order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order linkedOrder;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
}
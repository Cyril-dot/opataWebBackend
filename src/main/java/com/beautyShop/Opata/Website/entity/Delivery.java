package com.beautyShop.Opata.Website.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Linked order ─────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ── User who requested delivery ──────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── Admin managing this delivery ─────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_owner_id", nullable = false)
    private ShopOwner assignedAdmin;

    // ── Delivery address details ─────────────────────────────
    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    private String city;
    private String region;
    private String country;

    // Additional directions or landmarks
    @Column(columnDefinition = "TEXT")
    private String deliveryNotes;

    // ── Delivery status ──────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.REQUESTED;

    // ── Delivery fee ─────────────────────────────────────────
    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    // ── Tracking info ────────────────────────────────────────
    private String trackingNumber;
    private String courierName;  // e.g. "DHL", "GIG Logistics", "In-house"

    // ── Estimated and actual delivery times ──────────────────
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;

    // ── Chat room linked to this delivery ────────────────────
    // Created when admin opens a conversation about this delivery
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
package com.beautyShop.Opata.Website.dto;

import com.beautyShop.Opata.Website.entity.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ── Returned when viewing a delivery ────────────────────────
@Data
@Builder
public class DeliveryResponse {

    private Long id;

    // Order info
    private Long orderId;
    private String orderStatus;
    private BigDecimal orderTotal;

    // User info
    private String customerName;
    private String customerEmail;

    // Delivery address
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String city;
    private String region;
    private String country;
    private String deliveryNotes;

    // Status & tracking
    private DeliveryStatus status;
    private String trackingNumber;
    private String courierName;
    private BigDecimal deliveryFee;

    // Timing
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;

    // Chat room linked to this delivery (if admin opened one)
    private Long chatRoomId;

    // Admin managing this delivery
    private String assignedAdminName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
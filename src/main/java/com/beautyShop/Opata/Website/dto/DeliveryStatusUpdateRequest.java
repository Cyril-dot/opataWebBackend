package com.beautyShop.Opata.Website.dto;

import com.beautyShop.Opata.Website.entity.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ── Used by admin to update delivery status ──────────────────
@Data
public class DeliveryStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private DeliveryStatus status;

    // Optional fields admin can fill in when updating
    private String trackingNumber;
    private String courierName;
    private BigDecimal deliveryFee;
    private LocalDateTime estimatedDeliveryTime;

    // Optional note to send to user via chat when status changes
    private String messageToUser;
}
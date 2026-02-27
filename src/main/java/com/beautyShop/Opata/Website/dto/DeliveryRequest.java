package com.beautyShop.Opata.Website.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// ── Used when user requests a delivery for their order ───────
@Data
public class DeliveryRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @NotBlank(message = "Recipient phone is required")
    private String recipientPhone;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    private String city;
    private String region;

    @NotBlank(message = "Country is required")
    private String country;

    // Any special instructions e.g. "Blue gate, ask for John"
    private String deliveryNotes;
}
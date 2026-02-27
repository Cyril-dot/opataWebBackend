package com.beautyShop.Opata.Website.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;
    // Order is built from the user's active cart automatically
}
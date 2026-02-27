package com.beautyShop.Opata.Website.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartChatRequest {

    @NotNull(message = "Product ID is required")
    private Long productId; // chat is always started about a product
}
package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {

    private Long cartItemId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private String category;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal; // quantity * unitPrice
}
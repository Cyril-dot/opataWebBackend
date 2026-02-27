package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private String imageUrl;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}